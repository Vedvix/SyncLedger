package com.vedvix.syncledger.service;

import com.vedvix.syncledger.dto.*;
import com.vedvix.syncledger.exception.BadRequestException;
import com.vedvix.syncledger.exception.ForbiddenException;
import com.vedvix.syncledger.exception.ResourceNotFoundException;
import com.vedvix.syncledger.model.*;
import com.vedvix.syncledger.repository.InvoiceRepository;
import com.vedvix.syncledger.repository.OrganizationRepository;
import com.vedvix.syncledger.repository.UserRepository;
import com.vedvix.syncledger.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.beans.factory.annotation.Value;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for Invoice management with multi-tenant support.
 * 
 * @author vedvix
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final InvoiceProcessingService invoiceProcessingService;
    private final SubscriptionEmailService emailService;
    private final InvoiceAuditService invoiceAuditService;

    @Value("${storage.type:local}")
    private String storageType;

    // ─── Upload & Process ──────────────────────────────────────────────────

    /**
     * Upload a PDF, store in S3/local, create invoice, and trigger extraction.
     */
    @Transactional
    public InvoiceDTO uploadInvoice(MultipartFile file, UserPrincipal currentUser) {
        Organization org;
        if (currentUser.isSuperAdmin()) {
            // Super Admin needs an org — use first available
            org = organizationRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new BadRequestException("No organization found. Create one first."));
        } else {
            org = organizationRepository.findById(currentUser.getOrganizationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", currentUser.getOrganizationId()));
        }
        
        try {
            Invoice invoice = invoiceProcessingService.uploadAndProcess(org, file);
            return mapToDTO(invoice);
        } catch (Exception e) {
            log.error("Failed to upload invoice: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to upload invoice: " + e.getMessage());
        }
    }

    /**
     * Re-process an invoice that failed extraction.
     */
    @Transactional
    public InvoiceDTO reprocessInvoice(Long id, UserPrincipal currentUser) {
        Invoice invoice = findInvoiceWithAccessCheck(id, currentUser);

        if (invoice.getS3Key() == null || invoice.getS3Key().isBlank()) {
            throw new BadRequestException("Invoice has no uploaded file to reprocess");
        }

        log.info("Reprocessing invoice {} (current status: {})", id, invoice.getStatus());

        invoice.setStatus(InvoiceStatus.PENDING);
        invoice.setRequiresManualReview(false);
        invoice.setReviewNotes(null);
        invoiceRepository.save(invoice);

        invoiceProcessingService.processInvoiceAsync(id);

        Invoice updated = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));
        return mapToDTO(updated);
    }

    /**
     * Get invoices for current user's organization (or all for Super Admin).
     */
    @Transactional(readOnly = true)
    public PagedResponse<InvoiceDTO> getInvoices(Pageable pageable, String search, 
                                                  List<InvoiceStatus> statuses, UserPrincipal currentUser) {
        Page<Invoice> invoices;

        if (currentUser.isSuperAdmin()) {
            // Super Admin sees all invoices
            if (statuses != null && !statuses.isEmpty()) {
                if (statuses.size() == 1) {
                    invoices = invoiceRepository.findByStatus(statuses.get(0), pageable);
                } else {
                    invoices = invoiceRepository.findByStatusIn(statuses, pageable);
                }
            } else if (search != null && !search.isEmpty()) {
                invoices = invoiceRepository.searchInvoices(search, pageable);
            } else {
                invoices = invoiceRepository.findAll(pageable);
            }
        } else {
            // Org users see only their organization's invoices
            Long orgId = currentUser.getOrganizationId();
            if (statuses != null && !statuses.isEmpty()) {
                if (statuses.size() == 1) {
                    invoices = invoiceRepository.findByOrganization_IdAndStatus(orgId, statuses.get(0), pageable);
                } else {
                    invoices = invoiceRepository.findByOrganization_IdAndStatusIn(orgId, statuses, pageable);
                }
            } else if (search != null && !search.isEmpty()) {
                invoices = invoiceRepository.searchInvoicesInOrganization(orgId, search, pageable);
            } else {
                invoices = invoiceRepository.findByOrganization_Id(orgId, pageable);
            }
        }

        List<InvoiceDTO> content = invoices.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        Page<InvoiceDTO> invoiceDTOs = invoices.map(this::mapToDTO);
        return PagedResponse.from(invoiceDTOs);
    }

    /**
     * Get invoice by ID (with org access check).
     */
    @Transactional(readOnly = true)
    public InvoiceDTO getInvoiceById(Long id, UserPrincipal currentUser) {
        Invoice invoice = findInvoiceWithAccessCheck(id, currentUser);
        return mapToDTO(invoice);
    }

    /**
     * Update invoice.
     */
    @Transactional
    public InvoiceDTO updateInvoice(Long id, UpdateInvoiceRequest request, UserPrincipal currentUser) {
        Invoice invoice = findInvoiceWithAccessCheck(id, currentUser);

        if (!invoice.isEditable()) {
            throw new BadRequestException("Invoice cannot be edited in current status");
        }

        // Update fields
        if (request.getInvoiceNumber() != null) {
            invoice.setInvoiceNumber(request.getInvoiceNumber());
        }
        if (request.getVendorName() != null) {
            invoice.setVendorName(request.getVendorName());
        }
        if (request.getVendorAddress() != null) {
            invoice.setVendorAddress(request.getVendorAddress());
        }
        if (request.getSubtotal() != null) {
            invoice.setSubtotal(request.getSubtotal());
        }
        if (request.getTaxAmount() != null) {
            invoice.setTaxAmount(request.getTaxAmount());
        }
        if (request.getTotalAmount() != null) {
            invoice.setTotalAmount(request.getTotalAmount());
        }
        if (request.getInvoiceDate() != null) {
            invoice.setInvoiceDate(request.getInvoiceDate());
        }
        if (request.getDueDate() != null) {
            invoice.setDueDate(request.getDueDate());
        }
        if (request.getPoNumber() != null) {
            invoice.setPoNumber(request.getPoNumber());
        }
        if (request.getReviewNotes() != null) {
            invoice.setReviewNotes(request.getReviewNotes());
        }
        if (request.getGlAccount() != null) {
            invoice.setGlAccount(request.getGlAccount());
        }
        if (request.getProject() != null) {
            invoice.setProject(request.getProject());
        }
        if (request.getItemCategory() != null) {
            invoice.setItemCategory(request.getItemCategory());
        }
        if (request.getLocation() != null) {
            invoice.setLocation(request.getLocation());
        }
        if (request.getCostCenter() != null) {
            invoice.setCostCenter(request.getCostCenter());
        }

        invoiceRepository.save(invoice);
        log.info("Invoice {} updated by {}", invoice.getInvoiceNumber(), currentUser.getEmail());

        // Audit: fields updated
        java.util.Map<String, Object> changedFields = new java.util.HashMap<>();
        if (request.getInvoiceNumber() != null) changedFields.put("invoiceNumber", request.getInvoiceNumber());
        if (request.getVendorName() != null) changedFields.put("vendorName", request.getVendorName());
        if (request.getTotalAmount() != null) changedFields.put("totalAmount", request.getTotalAmount());
        if (request.getInvoiceDate() != null) changedFields.put("invoiceDate", request.getInvoiceDate().toString());
        if (request.getDueDate() != null) changedFields.put("dueDate", request.getDueDate().toString());
        if (request.getGlAccount() != null) changedFields.put("glAccount", request.getGlAccount());
        if (request.getProject() != null) changedFields.put("project", request.getProject());
        invoiceAuditService.logFieldsUpdated(invoice, currentUser, changedFields);

        return mapToDTO(invoice);
    }

    /**
     * Approve invoice.
     */
    @Transactional
    public InvoiceDTO approveInvoice(Long id, String notes, UserPrincipal currentUser) {
        Invoice invoice = findInvoiceWithAccessCheck(id, currentUser);

        if (invoice.getStatus() != InvoiceStatus.PENDING && invoice.getStatus() != InvoiceStatus.UNDER_REVIEW) {
            throw new BadRequestException("Invoice cannot be approved in current status");
        }

        // Create approval record
        User approver = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        Approval approval = Approval.builder()
                .invoice(invoice)
                .approver(approver)
                .action(ApprovalAction.APPROVED)
                .comments(notes)
                .build();
        invoice.addApproval(approval);
        
        invoice.setStatus(InvoiceStatus.APPROVED);
        invoice.setProcessedBy(approver);
        invoiceRepository.save(invoice);

        // Audit: approved
        invoiceAuditService.logApproved(invoice, approver, notes);

        log.info("Invoice {} approved by {}", invoice.getInvoiceNumber(), currentUser.getEmail());

        // Send approval notification email
        try {
            emailService.sendInvoiceApprovedEmail(
                    invoice.getOrganization(),
                    invoice.getInvoiceNumber(),
                    invoice.getVendorName(),
                    invoice.getTotalAmount() != null ? invoice.getTotalAmount().toPlainString() : "0.00",
                    invoice.getCurrency(),
                    approver.getFullName(),
                    notes,
                    invoice.getId()
            );
        } catch (Exception e) {
            log.warn("Failed to send invoice approval notification (non-blocking): {}", e.getMessage());
        }

        return mapToDTO(invoice);
    }

    /**
     * Reject invoice.
     */
    @Transactional
    public InvoiceDTO rejectInvoice(Long id, String reason, UserPrincipal currentUser) {
        Invoice invoice = findInvoiceWithAccessCheck(id, currentUser);

        if (invoice.getStatus() != InvoiceStatus.PENDING && invoice.getStatus() != InvoiceStatus.UNDER_REVIEW) {
            throw new BadRequestException("Invoice cannot be rejected in current status");
        }

        User approver = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        Approval approval = Approval.builder()
                .invoice(invoice)
                .approver(approver)
                .action(ApprovalAction.REJECTED)
                .comments(reason)
                .build();
        invoice.addApproval(approval);
        
        invoice.setStatus(InvoiceStatus.REJECTED);
        invoice.setProcessedBy(approver);
        invoice.setReviewNotes(reason);
        invoiceRepository.save(invoice);

        // Audit: rejected
        invoiceAuditService.logRejected(invoice, approver, reason);

        log.info("Invoice {} rejected by {}", invoice.getInvoiceNumber(), currentUser.getEmail());

        // Send rejection notification email
        try {
            emailService.sendInvoiceRejectedEmail(
                    invoice.getOrganization(),
                    invoice.getInvoiceNumber(),
                    invoice.getVendorName(),
                    invoice.getTotalAmount() != null ? invoice.getTotalAmount().toPlainString() : "0.00",
                    invoice.getCurrency(),
                    approver.getFullName(),
                    reason,
                    invoice.getId()
            );
        } catch (Exception e) {
            log.warn("Failed to send invoice rejection notification (non-blocking): {}", e.getMessage());
        }

        return mapToDTO(invoice);
    }

    /**
     * Get dashboard statistics for organization, optionally filtered by date range.
     */
    @Transactional(readOnly = true)
    public DashboardStatsDTO getDashboardStats(UserPrincipal currentUser, LocalDate startDate, LocalDate endDate) {
        boolean hasDateFilter = startDate != null && endDate != null;

        if (currentUser.isSuperAdmin()) {
            // Platform-wide stats
            List<Object[]> stats = hasDateFilter
                    ? invoiceRepository.getInvoiceStatsByStatusAndDateRange(startDate, endDate)
                    : invoiceRepository.getInvoiceStatsByStatus();
            List<Object[]> vendorStatusStats = hasDateFilter
                ? invoiceRepository.getVendorStatusBreakdownAndDateRange(startDate, endDate)
                : invoiceRepository.getVendorStatusBreakdown();
            return buildDashboardStats(stats, vendorStatusStats);
        } else {
            // Organization stats
            Long orgId = currentUser.getOrganizationId();
            List<Object[]> stats = hasDateFilter
                    ? invoiceRepository.getInvoiceStatsByStatusForOrganizationAndDateRange(orgId, startDate, endDate)
                    : invoiceRepository.getInvoiceStatsByStatusForOrganization(orgId);
            List<Object[]> vendorStatusStats = hasDateFilter
                ? invoiceRepository.getVendorStatusBreakdownForOrganizationAndDateRange(orgId, startDate, endDate)
                : invoiceRepository.getVendorStatusBreakdownForOrganization(orgId);
            return buildDashboardStats(stats, vendorStatusStats);
        }
    }

        private DashboardStatsDTO buildDashboardStats(List<Object[]> stats, List<Object[]> vendorStatusStats) {
        long totalInvoices = 0;
        long pendingCount = 0;
        long approvedCount = 0;
        long rejectedCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal pendingAmount = BigDecimal.ZERO;

        for (Object[] row : stats) {
            InvoiceStatus status = (InvoiceStatus) row[0];
            long count = ((Number) row[1]).longValue();
            BigDecimal amount = (BigDecimal) row[2];

            totalInvoices += count;
            totalAmount = totalAmount.add(amount);

            switch (status) {
                case PENDING:
                    pendingCount = count;
                    pendingAmount = amount;
                    break;
                case APPROVED:
                    approvedCount = count;
                    break;
                case REJECTED:
                    rejectedCount = count;
                    break;
                default:
                    break;
            }
        }

        List<DashboardStatsDTO.VendorStatusStats> vendorStatusBreakdown = buildVendorStatusBreakdown(vendorStatusStats);

        return DashboardStatsDTO.builder()
                .totalInvoices(totalInvoices)
                .pendingInvoices(pendingCount)
                .approvedInvoices(approvedCount)
                .rejectedInvoices(rejectedCount)
                .totalAmount(totalAmount)
                .pendingAmount(pendingAmount)
                .vendorStatusBreakdown(vendorStatusBreakdown)
                .build();
    }

    private List<DashboardStatsDTO.VendorStatusStats> buildVendorStatusBreakdown(List<Object[]> vendorStatusStats) {
        if (vendorStatusStats == null || vendorStatusStats.isEmpty()) {
            return List.of();
        }

        Map<String, Long> totalsByVendor = new LinkedHashMap<>();
        for (Object[] row : vendorStatusStats) {
            String vendorName = row[0] != null ? row[0].toString().trim() : "Unknown Vendor";
            long count = ((Number) row[2]).longValue();
            totalsByVendor.put(vendorName, totalsByVendor.getOrDefault(vendorName, 0L) + count);
        }

        return vendorStatusStats.stream()
                .map(row -> {
                    String vendorName = row[0] != null ? row[0].toString().trim() : "Unknown Vendor";
                    InvoiceStatus status = (InvoiceStatus) row[1];
                    long count = ((Number) row[2]).longValue();
                    return DashboardStatsDTO.VendorStatusStats.builder()
                            .vendorName(vendorName)
                            .status(status.name())
                            .invoiceCount(count)
                            .build();
                })
                .sorted((a, b) -> {
                    int byVendorTotal = Long.compare(
                            totalsByVendor.getOrDefault(b.getVendorName(), 0L),
                            totalsByVendor.getOrDefault(a.getVendorName(), 0L)
                    );
                    if (byVendorTotal != 0) return byVendorTotal;
                    return a.getStatus().compareTo(b.getStatus());
                })
                .collect(Collectors.toList());
    }

    /**
     * Find invoice with organization access check.
     */
    private Invoice findInvoiceWithAccessCheck(Long id, UserPrincipal currentUser) {
        Invoice invoice;
        
        if (currentUser.isSuperAdmin()) {
            invoice = invoiceRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));
        } else {
            invoice = invoiceRepository.findByIdAndOrganization_Id(id, currentUser.getOrganizationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));
        }
        
        return invoice;
    }

    private InvoiceDTO mapToDTO(Invoice invoice) {
        // Map line items
        List<InvoiceLineItemDTO> lineItemDTOs = invoice.getLineItems() != null
                ? invoice.getLineItems().stream()
                    .map(this::mapLineItemToDTO)
                    .collect(java.util.stream.Collectors.toList())
                : java.util.Collections.emptyList();

        // Compute days until due
        Long daysUntilDue = null;
        Boolean isOverdue = false;
        if (invoice.getDueDate() != null) {
            daysUntilDue = java.time.temporal.ChronoUnit.DAYS.between(
                    java.time.LocalDate.now(), invoice.getDueDate());
            isOverdue = daysUntilDue < 0;
        }

        return InvoiceDTO.builder()
                .id(invoice.getId())
                .organizationId(invoice.getOrganizationId())
                // Invoice Identification
                .invoiceNumber(invoice.getInvoiceNumber())
                .poNumber(invoice.getPoNumber())
                // Vendor Information
                .vendorId(invoice.getVendor() != null ? invoice.getVendor().getId() : null)
                .vendorName(invoice.getVendorName())
                .vendorAddress(invoice.getVendorAddress())
                .vendorEmail(invoice.getVendorEmail())
                .vendorPhone(invoice.getVendorPhone())
                .vendorTaxId(invoice.getVendorTaxId())
                // Financial Details
                .subtotal(invoice.getSubtotal())
                .taxAmount(invoice.getTaxAmount())
                .discountAmount(invoice.getDiscountAmount())
                .shippingAmount(invoice.getShippingAmount())
                .totalAmount(invoice.getTotalAmount())
                .currency(invoice.getCurrency())
                // Dates
                .invoiceDate(invoice.getInvoiceDate())
                .dueDate(invoice.getDueDate())
                .receivedDate(invoice.getReceivedDate())
                // Status & Processing
                .status(invoice.getStatus().name())
                .confidenceScore(invoice.getConfidenceScore())
                .requiresManualReview(invoice.getRequiresManualReview())
                .reviewNotes(invoice.getReviewNotes())
                // File Info
                .originalFileName(invoice.getOriginalFileName())
                .s3Url(getFreshFileUrl(invoice))
                .fileSizeBytes(invoice.getFileSizeBytes())
                .pageCount(invoice.getPageCount())
                // Email Source
                .sourceEmailFrom(invoice.getSourceEmailFrom())
                .sourceEmailSubject(invoice.getSourceEmailSubject())
                .sourceEmailReceivedAt(invoice.getSourceEmailReceivedAt())
                // Extraction
                .extractionMethod(invoice.getExtractionMethod())
                .extractedAt(invoice.getExtractedAt())
                // Sage Integration
                .sageInvoiceId(invoice.getSageInvoiceId())
                .syncStatus(invoice.getSyncStatus() != null ? invoice.getSyncStatus().name() : null)
                .lastSyncAttempt(invoice.getLastSyncAttempt())
                .syncErrorMessage(invoice.getSyncErrorMessage())
                // Line Items
                .lineItems(lineItemDTOs)
                // Mapping Fields
                .glAccount(invoice.getGlAccount())
                .project(invoice.getProject())
                .itemCategory(invoice.getItemCategory())
                .location(invoice.getLocation())
                .costCenter(invoice.getCostCenter())
                .mappingProfileId(invoice.getMappingProfileId())
                .fieldMappings(invoice.getFieldMappings())
                .rawExtractedData(invoice.getRawExtractedData())
                // Assignment
                .assignedToId(invoice.getAssignedTo() != null ? invoice.getAssignedTo().getId() : null)
                .assignedToName(invoice.getAssignedTo() != null
                        ? invoice.getAssignedTo().getFirstName() + " " + invoice.getAssignedTo().getLastName()
                        : null)
                // Audit
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                // Computed
                .daysUntilDue(daysUntilDue)
                .isOverdue(isOverdue)
                .isEditable(invoice.isEditable())
                .build();
    }

    private InvoiceLineItemDTO mapLineItemToDTO(InvoiceLineItem lineItem) {
        return InvoiceLineItemDTO.builder()
                .id(lineItem.getId())
                .lineNumber(lineItem.getLineNumber())
                .description(lineItem.getDescription())
                .itemCode(lineItem.getItemCode())
                .unit(lineItem.getUnit())
                .quantity(lineItem.getQuantity())
                .unitPrice(lineItem.getUnitPrice())
                .taxRate(lineItem.getTaxRate())
                .taxAmount(lineItem.getTaxAmount())
                .discountAmount(lineItem.getDiscountAmount())
                .lineTotal(lineItem.getLineTotal())
                .glAccountCode(lineItem.getGlAccountCode())
                .costCenter(lineItem.getCostCenter())
                .build();
    }

    /**
     * Generate fresh file URL for invoice preview.
     * For S3 storage: generates a new presigned URL (old ones expire after 1 hour).
     * For local storage: returns the stored URL as-is (no expiry).
     */
    private String getFreshFileUrl(Invoice invoice) {
        if (invoice.getS3Key() == null || invoice.getS3Key().isBlank()) {
            return invoice.getS3Url(); // fallback to stored URL
        }
        try {
            return storageService.generatePresignedUrl(invoice.getS3Key());
        } catch (Exception e) {
            log.warn("Failed to generate fresh URL for invoice {}, falling back to stored URL: {}", 
                     invoice.getId(), e.getMessage());
            return invoice.getS3Url();
        }
    }

    /**
     * Download invoice file as InputStream.
     */
    @Transactional(readOnly = true)
    public InputStream downloadInvoiceFile(Long id, UserPrincipal currentUser) {
        Invoice invoice = findInvoiceWithAccessCheck(id, currentUser);
        if (invoice.getS3Key() == null || invoice.getS3Key().isBlank()) {
            throw new ResourceNotFoundException("Invoice", "file", id);
        }
        return storageService.downloadFile(invoice.getS3Key());
    }

    /**
     * Get original filename for invoice.
     */
    @Transactional(readOnly = true)
    public String getInvoiceFileName(Long id, UserPrincipal currentUser) {
        Invoice invoice = findInvoiceWithAccessCheck(id, currentUser);
        return invoice.getOriginalFileName();
    }
}

