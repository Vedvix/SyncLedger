package com.vedvix.syncledger.service;

import com.vedvix.syncledger.dto.InvoiceDTO;
import com.vedvix.syncledger.exception.BadRequestException;
import com.vedvix.syncledger.exception.ResourceNotFoundException;
import com.vedvix.syncledger.model.*;
import com.vedvix.syncledger.repository.InvoiceRepository;
import com.vedvix.syncledger.repository.OrganizationRepository;
import com.vedvix.syncledger.repository.SageSyncRepository;
import com.vedvix.syncledger.repository.UserRepository;
import com.vedvix.syncledger.security.UserPrincipal;
import com.vedvix.syncledger.service.erp.ErpConnector;
import com.vedvix.syncledger.service.erp.ErpConnectorFactory;
import com.vedvix.syncledger.service.erp.ErpPropertyService;
import com.vedvix.syncledger.service.erp.ErpSyncResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ErpSyncService {

    private final InvoiceRepository invoiceRepository;
    private final OrganizationRepository organizationRepository;
    private final SageSyncRepository sageSyncRepository;
    private final UserRepository userRepository;
    private final InvoiceService invoiceService;
    private final ErpConnectorFactory erpConnectorFactory;
    private final ErpPropertyService erpPropertyService;

    @Transactional
    public InvoiceDTO syncInvoice(Long invoiceId, UserPrincipal currentUser) {
        // 1. Find invoice with org-scoped access check
        Invoice invoice;
        if (currentUser.isSuperAdmin()) {
            invoice = invoiceRepository.findById(invoiceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", invoiceId));
        } else {
            invoice = invoiceRepository.findByIdAndOrganization_Id(invoiceId, currentUser.getOrganizationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", invoiceId));
        }

        // 2. Validate invoice status — only APPROVED invoices can be synced
        if (invoice.getStatus() != InvoiceStatus.APPROVED) {
            throw new BadRequestException("Only approved invoices can be synced to ERP. Current status: " + invoice.getStatus());
        }

        // 3. Validate organization has ERP configured
        Organization org = invoice.getOrganization();
        if (org.getErpType() == null || org.getErpType() == ErpType.NONE) {
            throw new BadRequestException("No ERP integration configured for organization: " + org.getName()
                    + ". Please configure ERP settings first.");
        }

        // 4. Check not already synced
        if (invoice.getSyncStatus() == SyncStatus.SUCCESS) {
            throw new BadRequestException("Invoice is already synced to ERP");
        }

        // 5. Check not currently in progress
        if (invoice.getSyncStatus() == SyncStatus.IN_PROGRESS) {
            throw new BadRequestException("Invoice sync is already in progress");
        }

        // 6. Resolve the triggering user
        User triggeredBy = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        // 7. Mark as in-progress
        invoice.setSyncStatus(SyncStatus.IN_PROGRESS);
        invoice.setLastSyncAttempt(LocalDateTime.now());
        invoice.setSyncAttemptCount(
                invoice.getSyncAttemptCount() != null ? invoice.getSyncAttemptCount() + 1 : 1
        );
        invoice.setSyncErrorMessage(null);
        invoiceRepository.save(invoice);

        // 8. Create sync log entry
        long startTime = System.currentTimeMillis();
        SageSync syncLog = SageSync.builder()
                .invoice(invoice)
                .status(SyncStatus.IN_PROGRESS)
                .attemptNumber(invoice.getSyncAttemptCount())
                .triggeredBy(triggeredBy)
                .triggerType("MANUAL")
                .build();

        try {
            // 9. Resolve connector + properties and perform sync
            ErpConnector connector = erpConnectorFactory.getConnector(org.getErpType());
            Map<String, String> properties = erpPropertyService
                    .getDecryptedProperties(org.getId(), org.getErpType());

            ErpSyncResult result = connector.createBill(invoice, properties);

            // 10. Populate sync log
            syncLog.setRequestPayload(result.requestPayload());
            syncLog.setResponsePayload(result.responsePayload());
            syncLog.setHttpStatusCode(result.httpStatusCode());

            if (result.success()) {
                if (result.remoteRecordId() != null) {
                    invoice.setSageInvoiceId(result.remoteRecordId());
                    syncLog.setSageInvoiceId(result.remoteRecordId());
                } else {
                    log.warn("ERP sync returned success but no record ID for invoice {}", invoiceId);
                }

                long durationMs = System.currentTimeMillis() - startTime;
                invoice.setSyncStatus(SyncStatus.SUCCESS);
                invoice.setSyncErrorMessage(null);
                invoiceRepository.save(invoice);

                syncLog.setStatus(SyncStatus.SUCCESS);
                syncLog.setDurationMs((int) durationMs);
                sageSyncRepository.save(syncLog);

                log.info("Invoice {} synced successfully to {} in {}ms (remoteId={})",
                        invoiceId, org.getErpType(), durationMs, result.remoteRecordId());
            } else {
                syncLog.setErrorCode(result.errorCode());
                syncLog.setErrorMessage(result.errorMessage());
                throw new BadRequestException(result.errorMessage() != null
                        ? result.errorMessage()
                        : "ERP sync failed");
            }

        } catch (BadRequestException e) {
            long durationMs = System.currentTimeMillis() - startTime;
            log.error("Failed to sync invoice {} to {}: {}", invoiceId, org.getErpType(), e.getMessage());

            invoice.setSyncStatus(SyncStatus.FAILED);
            invoice.setSyncErrorMessage(e.getMessage());
            invoiceRepository.save(invoice);

            syncLog.setStatus(SyncStatus.FAILED);
            if (syncLog.getErrorMessage() == null) syncLog.setErrorMessage(e.getMessage());
            syncLog.setDurationMs((int) durationMs);
            sageSyncRepository.save(syncLog);

            throw e;
        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startTime;
            log.error("Failed to sync invoice {} to {}", invoiceId, org.getErpType(), e);

            invoice.setSyncStatus(SyncStatus.FAILED);
            invoice.setSyncErrorMessage("ERP sync failed. Please try again or contact support.");
            invoiceRepository.save(invoice);

            syncLog.setStatus(SyncStatus.FAILED);
            syncLog.setErrorMessage(e.getMessage());
            syncLog.setDurationMs((int) durationMs);
            sageSyncRepository.save(syncLog);

            throw new BadRequestException("ERP sync failed. Please try again or contact support.");
        }

        return invoiceService.getInvoiceById(invoiceId, currentUser);
    }
}
