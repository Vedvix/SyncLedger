package com.vedvix.syncledger.service;

import com.vedvix.syncledger.dto.InvoiceAuditEventDTO;
import com.vedvix.syncledger.model.*;
import com.vedvix.syncledger.repository.InvoiceAuditEventRepository;
import com.vedvix.syncledger.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for recording and retrieving invoice lifecycle audit events.
 * All logging methods are fire-and-forget — they never throw exceptions
 * to avoid disrupting the main invoice workflow.
 *
 * @author vedvix
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceAuditService {

    private final InvoiceAuditEventRepository auditEventRepository;

    // ═══════════════════════════════════════════════════════════════════════
    //  Query
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get the full audit trail for an invoice.
     */
    public List<InvoiceAuditEventDTO> getAuditTrail(Long invoiceId) {
        return auditEventRepository.findByInvoiceIdOrderByCreatedAtAsc(invoiceId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get the audit trail scoped by organization (multi-tenant).
     */
    public List<InvoiceAuditEventDTO> getAuditTrail(Long invoiceId, Long organizationId) {
        return auditEventRepository.findByInvoiceIdAndOrganizationIdOrderByCreatedAtAsc(invoiceId, organizationId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Convenience loggers — called from other services
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Invoice received via email polling.
     */
    public void logReceivedViaEmail(Invoice invoice, String emailFrom, String emailSubject) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("emailFrom", emailFrom);
        meta.put("emailSubject", emailSubject);
        meta.put("fileName", invoice.getOriginalFileName());

        saveEvent(invoice, InvoiceAuditEventType.RECEIVED_VIA_EMAIL,
                null, InvoiceStatus.PENDING,
                null, null,
                "Invoice received from email: " + emailFrom,
                meta);
    }

    /**
     * Invoice received via manual upload.
     */
    public void logReceivedViaUpload(Invoice invoice, UserPrincipal user) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("fileName", invoice.getOriginalFileName());

        saveEvent(invoice, InvoiceAuditEventType.RECEIVED_VIA_UPLOAD,
                null, InvoiceStatus.PENDING,
                user != null ? user.getId() : null,
                user != null ? user.getFullName() : null,
                "Invoice uploaded manually",
                meta);
    }

    /**
     * AI extraction started.
     */
    public void logExtractionStarted(Invoice invoice) {
        saveEvent(invoice, InvoiceAuditEventType.EXTRACTION_STARTED,
                invoice.getStatus(), null,
                null, null,
                "AI data extraction started",
                null);
    }

    /**
     * AI extraction completed successfully.
     */
    public void logExtractionCompleted(Invoice invoice, BigDecimal confidenceScore,
                                        String extractionMethod, int lineItemCount) {
        Map<String, Object> meta = new HashMap<>();
        if (confidenceScore != null) meta.put("confidenceScore", confidenceScore);
        if (extractionMethod != null) meta.put("extractionMethod", extractionMethod);
        meta.put("lineItemCount", lineItemCount);

        InvoiceStatus toStatus = (invoice.getRequiresManualReview() != null && invoice.getRequiresManualReview())
                ? InvoiceStatus.UNDER_REVIEW
                : InvoiceStatus.PENDING;

        saveEvent(invoice, InvoiceAuditEventType.EXTRACTION_COMPLETED,
                InvoiceStatus.PENDING, toStatus,
                null, null,
                "AI extraction completed — confidence: " +
                        (confidenceScore != null ? confidenceScore.toPlainString() : "N/A") +
                        ", " + lineItemCount + " line items extracted",
                meta);
    }

    /**
     * AI extraction failed.
     */
    public void logExtractionFailed(Invoice invoice, String errorMessage) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("error", errorMessage);

        saveEvent(invoice, InvoiceAuditEventType.EXTRACTION_FAILED,
                InvoiceStatus.PENDING, InvoiceStatus.UNDER_REVIEW,
                null, null,
                "AI extraction failed: " + truncate(errorMessage, 200),
                meta);
    }

    /**
     * Invoice approved.
     */
    public void logApproved(Invoice invoice, User approver, String notes) {
        Map<String, Object> meta = new HashMap<>();
        if (notes != null) meta.put("comments", notes);

        saveEvent(invoice, InvoiceAuditEventType.APPROVED,
                InvoiceStatus.UNDER_REVIEW, InvoiceStatus.APPROVED,
                approver.getId(), approver.getFullName(),
                "Invoice approved by " + approver.getFullName(),
                meta.isEmpty() ? null : meta);
    }

    /**
     * Invoice rejected.
     */
    public void logRejected(Invoice invoice, User approver, String reason) {
        Map<String, Object> meta = new HashMap<>();
        if (reason != null) meta.put("rejectionReason", reason);

        saveEvent(invoice, InvoiceAuditEventType.REJECTED,
                InvoiceStatus.UNDER_REVIEW, InvoiceStatus.REJECTED,
                approver.getId(), approver.getFullName(),
                "Invoice rejected by " + approver.getFullName() +
                        (reason != null ? ": " + truncate(reason, 200) : ""),
                meta.isEmpty() ? null : meta);
    }

    /**
     * Invoice fields updated.
     */
    public void logFieldsUpdated(Invoice invoice, UserPrincipal user, Map<String, Object> changedFields) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("changedFields", changedFields);

        saveEvent(invoice, InvoiceAuditEventType.FIELD_UPDATED,
                invoice.getStatus(), invoice.getStatus(),
                user.getId(), user.getFullName(),
                "Invoice fields updated by " + user.getFullName(),
                meta);
    }

    /**
     * ERP sync started.
     */
    public void logSyncStarted(Invoice invoice, UserPrincipal user) {
        saveEvent(invoice, InvoiceAuditEventType.SYNC_STARTED,
                invoice.getStatus(), invoice.getStatus(),
                user != null ? user.getId() : null,
                user != null ? user.getFullName() : null,
                "ERP synchronization initiated",
                null);
    }

    /**
     * ERP sync completed.
     */
    public void logSyncCompleted(Invoice invoice, String erpInvoiceId) {
        Map<String, Object> meta = new HashMap<>();
        if (erpInvoiceId != null) meta.put("erpInvoiceId", erpInvoiceId);

        saveEvent(invoice, InvoiceAuditEventType.SYNC_COMPLETED,
                InvoiceStatus.APPROVED, InvoiceStatus.SYNCED,
                null, null,
                "Successfully synced to ERP" +
                        (erpInvoiceId != null ? " (ID: " + erpInvoiceId + ")" : ""),
                meta.isEmpty() ? null : meta);
    }

    /**
     * ERP sync failed.
     */
    public void logSyncFailed(Invoice invoice, String errorMessage) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("error", errorMessage);

        saveEvent(invoice, InvoiceAuditEventType.SYNC_FAILED,
                InvoiceStatus.APPROVED, InvoiceStatus.SYNC_FAILED,
                null, null,
                "ERP synchronization failed: " + truncate(errorMessage, 200),
                meta);
    }

    /**
     * Vendor auto-linked from extraction.
     */
    public void logVendorLinked(Invoice invoice, String vendorName, Long vendorId) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("vendorName", vendorName);
        meta.put("vendorId", vendorId);

        saveEvent(invoice, InvoiceAuditEventType.VENDOR_LINKED,
                invoice.getStatus(), invoice.getStatus(),
                null, null,
                "Vendor auto-linked: " + vendorName,
                meta);
    }

    /**
     * Invoice assigned to a user.
     */
    public void logAssigned(Invoice invoice, User assignee, UserPrincipal assignedBy) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("assigneeId", assignee.getId());
        meta.put("assigneeName", assignee.getFullName());

        saveEvent(invoice, InvoiceAuditEventType.ASSIGNED,
                invoice.getStatus(), invoice.getStatus(),
                assignedBy != null ? assignedBy.getId() : null,
                assignedBy != null ? assignedBy.getFullName() : null,
                "Invoice assigned to " + assignee.getFullName(),
                meta);
    }

    /**
     * Invoice archived.
     */
    public void logArchived(Invoice invoice, UserPrincipal user) {
        saveEvent(invoice, InvoiceAuditEventType.ARCHIVED,
                invoice.getStatus(), InvoiceStatus.ARCHIVED,
                user != null ? user.getId() : null,
                user != null ? user.getFullName() : null,
                "Invoice archived",
                null);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Internal
    // ═══════════════════════════════════════════════════════════════════════

    private void saveEvent(Invoice invoice, InvoiceAuditEventType eventType,
                           InvoiceStatus fromStatus, InvoiceStatus toStatus,
                           Long userId, String userName,
                           String description, Map<String, Object> metadata) {
        try {
            InvoiceAuditEvent event = InvoiceAuditEvent.builder()
                    .invoiceId(invoice.getId())
                    .organizationId(invoice.getOrganizationId())
                    .eventType(eventType)
                    .fromStatus(fromStatus)
                    .toStatus(toStatus)
                    .performedByUserId(userId)
                    .performedByName(userName)
                    .description(description)
                    .metadata(metadata)
                    .build();

            auditEventRepository.save(event);
            log.debug("Audit event saved: invoice={}, type={}", invoice.getId(), eventType);
        } catch (Exception e) {
            // Never let audit logging break the main flow
            log.warn("Failed to save audit event for invoice {}: {}", invoice.getId(), e.getMessage());
        }
    }

    private InvoiceAuditEventDTO toDTO(InvoiceAuditEvent event) {
        return InvoiceAuditEventDTO.builder()
                .id(event.getId())
                .invoiceId(event.getInvoiceId())
                .eventType(event.getEventType().name())
                .eventDisplayName(event.getEventType().getDisplayName())
                .fromStatus(event.getFromStatus() != null ? event.getFromStatus().name() : null)
                .toStatus(event.getToStatus() != null ? event.getToStatus().name() : null)
                .performedByUserId(event.getPerformedByUserId())
                .performedByName(event.getPerformedByName())
                .description(event.getDescription())
                .metadata(event.getMetadata())
                .createdAt(event.getCreatedAt())
                .build();
    }

    private static String truncate(String str, int maxLen) {
        if (str == null) return null;
        return str.length() > maxLen ? str.substring(0, maxLen) + "…" : str;
    }
}
