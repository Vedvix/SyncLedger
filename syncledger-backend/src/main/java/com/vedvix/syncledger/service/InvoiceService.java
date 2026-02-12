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
import java.util.List;
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

        log.info("Invoice {} approved by {}", invoice.getInvoiceNumber(), currentUser.getEmail());
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

        log.info("Invoice {} rejected by {}", invoice.getInvoiceNumber(), currentUser.getEmail());
        return mapToDTO(invoice);
    }

    /**
     * Get dashboard statistics for organization.
     */
    @Transactional(readOnly = true)
    public DashboardStatsDTO getDashboardStats(UserPrincipal currentUser) {
        if (currentUser.isSuperAdmin()) {
            // Platform-wide stats
            List<Object[]> stats = invoiceRepository.getInvoiceStatsByStatus();
            return buildDashboardStats(stats);
        } else {
            // Organization stats
            Long orgId = currentUser.getOrganizationId();
            List<Object[]> stats = invoiceRepository.getInvoiceStatsByStatusForOrganization(orgId);
            return buildDashboardStats(stats);
        }
    }

    private DashboardStatsDTO buildDashboardStats(List<Object[]> stats) {
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

        return DashboardStatsDTO.builder()
                .totalInvoices(totalInvoices)
                .pendingInvoices(pendingCount)
                .approvedInvoices(approvedCount)
                .rejectedInvoices(rejectedCount)
                .totalAmount(totalAmount)
                .pendingAmount(pendingAmount)
                .build();
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
    public String getInvoiceFileName(Long id, UserPrincipal currentUser) {
        Invoice invoice = findInvoiceWithAccessCheck(id, currentUser);
        return invoice.getOriginalFileName();
    }
}

