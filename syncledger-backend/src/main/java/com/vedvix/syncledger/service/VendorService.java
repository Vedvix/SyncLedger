package com.vedvix.syncledger.service;

import com.vedvix.syncledger.dto.*;
import com.vedvix.syncledger.exception.BadRequestException;
import com.vedvix.syncledger.exception.ResourceNotFoundException;
import com.vedvix.syncledger.model.*;
import com.vedvix.syncledger.repository.InvoiceRepository;
import com.vedvix.syncledger.repository.OrganizationRepository;
import com.vedvix.syncledger.repository.VendorRepository;
import com.vedvix.syncledger.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for Vendor management with multi-tenant support and analytics.
 * 
 * @author vedvix
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository vendorRepository;
    private final InvoiceRepository invoiceRepository;
    private final OrganizationRepository organizationRepository;

    // ─── CRUD Operations ──────────────────────────────────────────────────

    /**
     * Get paginated list of vendors for the current user's organization.
     */
    @Transactional(readOnly = true)
    public PagedResponse<VendorDTO> getVendors(Pageable pageable, String search, UserPrincipal currentUser) {
        Page<Vendor> vendors;

        if (currentUser.isSuperAdmin()) {
            if (search != null && !search.isEmpty()) {
                vendors = vendorRepository.searchAllVendors(search, pageable);
            } else {
                vendors = vendorRepository.findAll(pageable);
            }
        } else {
            Long orgId = currentUser.getOrganizationId();
            if (search != null && !search.isEmpty()) {
                vendors = vendorRepository.searchVendorsInOrganization(orgId, search, pageable);
            } else {
                vendors = vendorRepository.findByOrganization_Id(orgId, pageable);
            }
        }

        Page<VendorDTO> dtoPage = vendors.map(this::mapToDTO);
        return PagedResponse.from(dtoPage);
    }

    /**
     * Get a single vendor by ID with access check.
     */
    @Transactional(readOnly = true)
    public VendorDTO getVendorById(Long id, UserPrincipal currentUser) {
        Vendor vendor = findVendorWithAccessCheck(id, currentUser);
        VendorDTO dto = mapToDTO(vendor);
        // Include analytics for single vendor view
        dto.setAnalytics(getVendorAnalytics(vendor.getId()));
        return dto;
    }

    /**
     * Create a new vendor.
     */
    @Transactional
    public VendorDTO createVendor(VendorRequest request, UserPrincipal currentUser) {
        Long orgId;
        if (currentUser.isSuperAdmin()) {
            orgId = organizationRepository.findAll().stream().findFirst()
                    .map(Organization::getId)
                    .orElseThrow(() -> new BadRequestException("No organization found"));
        } else {
            orgId = currentUser.getOrganizationId();
        }

        String normalizedName = Vendor.normalizeName(request.getName());
        if (vendorRepository.existsByOrganization_IdAndNormalizedName(orgId, normalizedName)) {
            throw new BadRequestException("Vendor with name '" + request.getName() + "' already exists in this organization");
        }

        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", orgId));

        Vendor vendor = Vendor.builder()
                .organization(org)
                .name(request.getName().trim())
                .normalizedName(normalizedName)
                .code(request.getCode())
                .address(request.getAddress())
                .email(request.getEmail())
                .phone(request.getPhone())
                .contactPerson(request.getContactPerson())
                .website(request.getWebsite())
                .taxId(request.getTaxId())
                .paymentTerms(request.getPaymentTerms())
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .notes(request.getNotes())
                .build();

        if (request.getStatus() != null) {
            vendor.setStatus(VendorStatus.valueOf(request.getStatus()));
        }

        vendorRepository.save(vendor);
        log.info("Created vendor: {} (id={}) for org {}", vendor.getName(), vendor.getId(), orgId);
        return mapToDTO(vendor);
    }

    /**
     * Update an existing vendor.
     */
    @Transactional
    public VendorDTO updateVendor(Long id, VendorRequest request, UserPrincipal currentUser) {
        Vendor vendor = findVendorWithAccessCheck(id, currentUser);

        if (request.getName() != null) {
            String newNormalized = Vendor.normalizeName(request.getName());
            if (!newNormalized.equals(vendor.getNormalizedName())) {
                if (vendorRepository.existsByOrganization_IdAndNormalizedName(vendor.getOrganizationId(), newNormalized)) {
                    throw new BadRequestException("Vendor with name '" + request.getName() + "' already exists");
                }
                vendor.setName(request.getName().trim());
                vendor.setNormalizedName(newNormalized);
            }
        }

        if (request.getCode() != null) vendor.setCode(request.getCode());
        if (request.getAddress() != null) vendor.setAddress(request.getAddress());
        if (request.getEmail() != null) vendor.setEmail(request.getEmail());
        if (request.getPhone() != null) vendor.setPhone(request.getPhone());
        if (request.getContactPerson() != null) vendor.setContactPerson(request.getContactPerson());
        if (request.getWebsite() != null) vendor.setWebsite(request.getWebsite());
        if (request.getTaxId() != null) vendor.setTaxId(request.getTaxId());
        if (request.getPaymentTerms() != null) vendor.setPaymentTerms(request.getPaymentTerms());
        if (request.getCurrency() != null) vendor.setCurrency(request.getCurrency());
        if (request.getNotes() != null) vendor.setNotes(request.getNotes());
        if (request.getStatus() != null) vendor.setStatus(VendorStatus.valueOf(request.getStatus()));

        vendorRepository.save(vendor);
        log.info("Updated vendor: {} (id={})", vendor.getName(), vendor.getId());
        return mapToDTO(vendor);
    }

    /**
     * Delete a vendor (only if no invoices linked).
     */
    @Transactional
    public void deleteVendor(Long id, UserPrincipal currentUser) {
        Vendor vendor = findVendorWithAccessCheck(id, currentUser);
        Long invoiceCount = vendorRepository.countInvoicesByVendor(vendor.getId());
        if (invoiceCount > 0) {
            throw new BadRequestException("Cannot delete vendor with " + invoiceCount + " linked invoices. Deactivate instead.");
        }
        vendorRepository.delete(vendor);
        log.info("Deleted vendor: {} (id={})", vendor.getName(), vendor.getId());
    }

    // ─── Auto-match: Find or Create Vendor ──────────────────────────────

    /**
     * Find existing vendor by name within org, or create a new one.
     * Called during invoice extraction to auto-link invoices to vendors.
     */
    @Transactional
    public Vendor findOrCreateVendor(Long orgId, String vendorName, String address, 
                                      String email, String phone, String taxId) {
        if (vendorName == null || vendorName.trim().isEmpty() || "Pending Extraction".equals(vendorName)) {
            return null;
        }

        String normalizedName = Vendor.normalizeName(vendorName);
        if (normalizedName.isEmpty()) return null;

        Optional<Vendor> existing = vendorRepository.findByOrganization_IdAndNormalizedName(orgId, normalizedName);
        
        if (existing.isPresent()) {
            Vendor vendor = existing.get();
            // Update contact details if we have new data and they were previously empty
            boolean updated = false;
            if (address != null && vendor.getAddress() == null) { vendor.setAddress(address); updated = true; }
            if (email != null && vendor.getEmail() == null) { vendor.setEmail(email); updated = true; }
            if (phone != null && vendor.getPhone() == null) { vendor.setPhone(phone); updated = true; }
            if (taxId != null && vendor.getTaxId() == null) { vendor.setTaxId(taxId); updated = true; }
            if (updated) {
                vendorRepository.save(vendor);
                log.debug("Updated vendor contact details for: {} (id={})", vendor.getName(), vendor.getId());
            }
            log.debug("Matched existing vendor: {} (id={}) for org {}", vendor.getName(), vendor.getId(), orgId);
            return vendor;
        }

        // Create new vendor
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", orgId));

        Vendor vendor = Vendor.builder()
                .organization(org)
                .name(vendorName.trim())
                .normalizedName(normalizedName)
                .address(address)
                .email(email)
                .phone(phone)
                .taxId(taxId)
                .build();

        vendorRepository.save(vendor);
        log.info("Auto-created vendor: {} (id={}) for org {}", vendor.getName(), vendor.getId(), orgId);
        return vendor;
    }

    // ─── Analytics ──────────────────────────────────────────────────────

    /**
     * Get detailed analytics for a specific vendor.
     */
    @Transactional(readOnly = true)
    public VendorAnalyticsDTO getVendorAnalytics(Long vendorId) {
        Long totalInvoices = vendorRepository.countInvoicesByVendor(vendorId);
        BigDecimal totalAmount = vendorRepository.sumTotalAmountByVendor(vendorId);
        BigDecimal avgAmount = vendorRepository.avgTotalAmountByVendor(vendorId);
        BigDecimal minAmount = vendorRepository.minTotalAmountByVendor(vendorId);
        BigDecimal maxAmount = vendorRepository.maxTotalAmountByVendor(vendorId);
        BigDecimal totalTax = vendorRepository.sumTaxAmountByVendor(vendorId);
        BigDecimal avgConfidence = vendorRepository.avgConfidenceByVendor(vendorId);

        Long pendingCount = vendorRepository.countInvoicesByVendorAndStatus(vendorId, InvoiceStatus.PENDING);
        Long approvedCount = vendorRepository.countInvoicesByVendorAndStatus(vendorId, InvoiceStatus.APPROVED);
        Long rejectedCount = vendorRepository.countInvoicesByVendorAndStatus(vendorId, InvoiceStatus.REJECTED);
        Long syncedCount = vendorRepository.countInvoicesByVendorAndStatus(vendorId, InvoiceStatus.SYNCED);
        Long reviewCount = vendorRepository.countInvoicesRequiringReviewByVendor(vendorId);

        LocalDate firstDate = vendorRepository.findFirstInvoiceDateByVendor(vendorId);
        LocalDate lastDate = vendorRepository.findLastInvoiceDateByVendor(vendorId);

        // Monthly totals for last 12 months
        LocalDate startDate = LocalDate.now().minusMonths(12);
        List<Object[]> monthlyRaw = vendorRepository.findMonthlyTotalsByVendor(vendorId, startDate);
        Map<String, BigDecimal> monthlyTotals = new LinkedHashMap<>();
        for (Object[] row : monthlyRaw) {
            monthlyTotals.put((String) row[0], (BigDecimal) row[1]);
        }

        return VendorAnalyticsDTO.builder()
                .totalInvoices(totalInvoices)
                .pendingInvoices(pendingCount)
                .approvedInvoices(approvedCount)
                .rejectedInvoices(rejectedCount)
                .syncedInvoices(syncedCount)
                .totalAmount(totalAmount)
                .averageInvoiceAmount(avgAmount.setScale(2, RoundingMode.HALF_UP))
                .minInvoiceAmount(minAmount)
                .maxInvoiceAmount(maxAmount)
                .totalTaxAmount(totalTax)
                .averageConfidenceScore(avgConfidence.setScale(2, RoundingMode.HALF_UP))
                .invoicesRequiringReview(reviewCount)
                .firstInvoiceDate(firstDate)
                .lastInvoiceDate(lastDate)
                .monthlyTotals(monthlyTotals)
                .build();
    }

    /**
     * Get organization-wide vendor summary (top vendors, totals).
     */
    @Transactional(readOnly = true)
    public VendorSummaryDTO getVendorSummary(UserPrincipal currentUser) {
        Long orgId;
        if (currentUser.isSuperAdmin()) {
            // For super admin, aggregate across all orgs (or first org)
            orgId = organizationRepository.findAll().stream().findFirst()
                    .map(Organization::getId).orElse(0L);
        } else {
            orgId = currentUser.getOrganizationId();
        }

        Long totalVendors = vendorRepository.countByOrganization_Id(orgId);
        Long activeVendors = vendorRepository.countByOrganization_IdAndStatus(orgId, VendorStatus.ACTIVE);
        Long totalInvoices = vendorRepository.countInvoicesAcrossVendors(orgId);
        BigDecimal totalAmount = vendorRepository.sumTotalAmountAcrossVendors(orgId);
        BigDecimal avgPerVendor = totalVendors > 0 
                ? totalAmount.divide(BigDecimal.valueOf(totalVendors), 2, RoundingMode.HALF_UP) 
                : BigDecimal.ZERO;

        // Top 5 by count
        List<VendorSummaryDTO.TopVendorDTO> topByCount = vendorRepository.findTopVendorsByInvoiceCount(orgId, 5)
                .stream().map(this::mapToTopVendor).collect(Collectors.toList());

        // Top 5 by amount
        List<VendorSummaryDTO.TopVendorDTO> topByAmount = vendorRepository.findTopVendorsByTotalAmount(orgId, 5)
                .stream().map(this::mapToTopVendor).collect(Collectors.toList());

        return VendorSummaryDTO.builder()
                .totalVendors(totalVendors)
                .activeVendors(activeVendors)
                .totalInvoicesAcrossVendors(totalInvoices)
                .totalAmountAcrossVendors(totalAmount)
                .averageAmountPerVendor(avgPerVendor)
                .topVendorsByCount(topByCount)
                .topVendorsByAmount(topByAmount)
                .build();
    }

    // ─── Private Helpers ────────────────────────────────────────────────

    private Vendor findVendorWithAccessCheck(Long id, UserPrincipal currentUser) {
        if (currentUser.isSuperAdmin()) {
            return vendorRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor", "id", id));
        }
        return vendorRepository.findByIdAndOrganization_Id(id, currentUser.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", "id", id));
    }

    private VendorDTO mapToDTO(Vendor vendor) {
        return VendorDTO.builder()
                .id(vendor.getId())
                .organizationId(vendor.getOrganizationId())
                .organizationName(vendor.getOrganization() != null ? vendor.getOrganization().getName() : null)
                .name(vendor.getName())
                .code(vendor.getCode())
                .address(vendor.getAddress())
                .email(vendor.getEmail())
                .phone(vendor.getPhone())
                .contactPerson(vendor.getContactPerson())
                .website(vendor.getWebsite())
                .taxId(vendor.getTaxId())
                .paymentTerms(vendor.getPaymentTerms())
                .currency(vendor.getCurrency())
                .status(vendor.getStatus().name())
                .notes(vendor.getNotes())
                .createdAt(vendor.getCreatedAt())
                .updatedAt(vendor.getUpdatedAt())
                .build();
    }

    private VendorSummaryDTO.TopVendorDTO mapToTopVendor(Object[] row) {
        return VendorSummaryDTO.TopVendorDTO.builder()
                .vendorId(((Number) row[0]).longValue())
                .vendorName((String) row[1])
                .invoiceCount(((Number) row[2]).longValue())
                .totalAmount((BigDecimal) row[3])
                .averageAmount((BigDecimal) row[4])
                .build();
    }
}
