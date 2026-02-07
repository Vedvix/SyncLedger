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

    /**
     * Get invoices for current user's organization (or all for Super Admin).
     */
    @Transactional(readOnly = true)
    public PagedResponse<InvoiceDTO> getInvoices(Pageable pageable, String search, 
                                                  InvoiceStatus status, UserPrincipal currentUser) {
        Page<Invoice> invoices;

        if (currentUser.isSuperAdmin()) {
            // Super Admin sees all invoices
            if (status != null) {
                invoices = invoiceRepository.findByStatus(status, pageable);
            } else if (search != null && !search.isEmpty()) {
                invoices = invoiceRepository.searchInvoices(search, pageable);
            } else {
                invoices = invoiceRepository.findAll(pageable);
            }
        } else {
            // Org users see only their organization's invoices
            Long orgId = currentUser.getOrganizationId();
            if (status != null) {
                invoices = invoiceRepository.findByOrganization_IdAndStatus(orgId, status, pageable);
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
        return InvoiceDTO.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .poNumber(invoice.getPoNumber())
                .vendorName(invoice.getVendorName())
                .vendorAddress(invoice.getVendorAddress())
                .vendorEmail(invoice.getVendorEmail())
                .subtotal(invoice.getSubtotal())
                .taxAmount(invoice.getTaxAmount())
                .totalAmount(invoice.getTotalAmount())
                .currency(invoice.getCurrency())
                .invoiceDate(invoice.getInvoiceDate())
                .dueDate(invoice.getDueDate())
                .status(invoice.getStatus().name())
                .confidenceScore(invoice.getConfidenceScore())
                .requiresManualReview(invoice.getRequiresManualReview())
                .reviewNotes(invoice.getReviewNotes())
                .originalFileName(invoice.getOriginalFileName())
                .s3Url(invoice.getS3Url())
                .organizationId(invoice.getOrganizationId())
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .build();
    }
}

