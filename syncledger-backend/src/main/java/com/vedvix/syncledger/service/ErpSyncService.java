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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ErpSyncService {

    private final InvoiceRepository invoiceRepository;
    private final OrganizationRepository organizationRepository;
    private final SageSyncRepository sageSyncRepository;
    private final UserRepository userRepository;
    private final InvoiceService invoiceService;
    private final SageIntacctService sageIntacctService;

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
            // 9. Perform the actual ERP sync based on type
            performErpSync(invoice, org, syncLog);

            // 10. Mark success
            long durationMs = System.currentTimeMillis() - startTime;
            invoice.setSyncStatus(SyncStatus.SUCCESS);
            invoice.setSyncErrorMessage(null);
            invoiceRepository.save(invoice);

            syncLog.setStatus(SyncStatus.SUCCESS);
            syncLog.setDurationMs((int) durationMs);
            sageSyncRepository.save(syncLog);

            log.info("Invoice {} synced successfully to {} in {}ms",
                    invoiceId, org.getErpType(), durationMs);

        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startTime;
            log.error("Failed to sync invoice {} to {}: {}", invoiceId, org.getErpType(), e.getMessage());

            invoice.setSyncStatus(SyncStatus.FAILED);
            invoice.setSyncErrorMessage(e.getMessage());
            invoiceRepository.save(invoice);

            syncLog.setStatus(SyncStatus.FAILED);
            syncLog.setErrorMessage(e.getMessage());
            syncLog.setDurationMs((int) durationMs);
            sageSyncRepository.save(syncLog);

            throw new BadRequestException("ERP sync failed: " + e.getMessage());
        }

        return invoiceService.getInvoiceById(invoiceId, currentUser);
    }

    private void performErpSync(Invoice invoice, Organization org, SageSync syncLog) {
        switch (org.getErpType()) {
            case SAGE:
                syncToSage(invoice, org, syncLog);
                break;
            case QUICKBOOKS:
            case NETSUITE:
            case ORACLE:
            case SAP:
            case XERO:
            case CUSTOM:
                throw new BadRequestException(org.getErpType().getDisplayName()
                        + " integration is not yet implemented. Coming soon!");
            case NONE:
            default:
                throw new BadRequestException("No ERP integration configured");
        }
    }

    private void syncToSage(Invoice invoice, Organization org, SageSync syncLog) {
        SageIntacctService.SageResponse response = sageIntacctService.createBill(invoice, org);

        // Populate sync log with request/response payloads
        syncLog.setRequestPayload(response.requestPayload());
        syncLog.setResponsePayload(response.responsePayload());
        syncLog.setHttpStatusCode(response.httpStatusCode());

        if (response.success()) {
            // Store Sage record number on the invoice
            if (response.recordNo() != null) {
                invoice.setSageInvoiceId(response.recordNo());
                syncLog.setSageInvoiceId(response.recordNo());
            }
        } else {
            syncLog.setErrorCode(response.errorCode());
            syncLog.setErrorMessage(response.errorMessage());
            throw new BadRequestException(response.errorMessage() != null
                    ? response.errorMessage()
                    : "Sage Intacct sync failed");
        }
    }
}
