package com.vedvix.syncledger.specification;

import com.vedvix.syncledger.dto.ExportRequest;
import com.vedvix.syncledger.model.Invoice;
import com.vedvix.syncledger.model.InvoiceStatus;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specification builder for Invoice entity.
 * Supports dynamic filtering for export and advanced queries.
 * 
 * @author vedvix
 */
public class InvoiceSpecification {

    private InvoiceSpecification() {
        // Utility class
    }

    /**
     * Build a specification for the invoice listing page.
     * Combines all filters (org scoping, status, search, vendor, date range) into one query.
     *
     * @param organizationId Organization ID for scoping (null for super admin / platform-wide)
     * @param statuses       Status filter list (null or empty for all statuses)
     * @param search         Free-text search across invoice number, vendor, PO number
     * @param vendorName     Vendor name filter
     * @param dateFrom       Invoice date range start
     * @param dateTo         Invoice date range end
     * @return Specification for querying invoices
     */
    public static Specification<Invoice> buildListingSpec(
            Long organizationId,
            List<InvoiceStatus> statuses,
            String search,
            String vendorName,
            LocalDate dateFrom,
            LocalDate dateTo) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Organization scoping (null = super admin sees all)
            if (organizationId != null) {
                predicates.add(cb.equal(root.get("organization").get("id"), organizationId));
            }

            // Status filter
            if (statuses != null && !statuses.isEmpty()) {
                predicates.add(root.get("status").in(statuses));
            }

            // Free-text search (across invoice number, vendor name, PO number)
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("invoiceNumber")), pattern),
                        cb.like(cb.lower(root.get("vendorName")), pattern),
                        cb.like(cb.lower(root.get("poNumber")), pattern)
                ));
            }

            // Vendor name filter
            if (vendorName != null && !vendorName.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("vendorName")),
                        "%" + vendorName.toLowerCase() + "%"));
            }

            // Date range filter
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("invoiceDate"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("invoiceDate"), dateTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Build a specification from an ExportRequest with organization scoping.
     * 
     * @param request The export filter criteria
     * @param organizationId Organization ID for scoping (null for super admin)
     * @return Specification for querying invoices
     */
    public static Specification<Invoice> fromExportRequest(ExportRequest request, Long organizationId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // ==================== Organization Scoping ====================
            if (organizationId != null) {
                predicates.add(cb.equal(root.get("organization").get("id"), organizationId));
            }

            // ==================== Basic Filters ====================
            if (request.getSearch() != null && !request.getSearch().isBlank()) {
                String searchPattern = "%" + request.getSearch().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("invoiceNumber")), searchPattern),
                        cb.like(cb.lower(root.get("vendorName")), searchPattern),
                        cb.like(cb.lower(root.get("poNumber")), searchPattern),
                        cb.like(cb.lower(root.get("originalFileName")), searchPattern)
                ));
            }

            if (request.getStatuses() != null && !request.getStatuses().isEmpty()) {
                List<InvoiceStatus> statusEnums = request.getStatuses().stream()
                        .map(s -> InvoiceStatus.valueOf(s.toUpperCase()))
                        .toList();
                predicates.add(root.get("status").in(statusEnums));
            }

            // ==================== Date Filters ====================
            if (request.getInvoiceDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("invoiceDate"), request.getInvoiceDateFrom()));
            }
            if (request.getInvoiceDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("invoiceDate"), request.getInvoiceDateTo()));
            }

            if (request.getDueDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dueDate"), request.getDueDateFrom()));
            }
            if (request.getDueDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dueDate"), request.getDueDateTo()));
            }

            if (request.getCreatedDateFrom() != null) {
                LocalDateTime from = request.getCreatedDateFrom().atStartOfDay();
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (request.getCreatedDateTo() != null) {
                LocalDateTime to = request.getCreatedDateTo().plusDays(1).atStartOfDay();
                predicates.add(cb.lessThan(root.get("createdAt"), to));
            }

            // ==================== Financial Filters ====================
            if (request.getMinAmount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("totalAmount"), request.getMinAmount()));
            }
            if (request.getMaxAmount() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("totalAmount"), request.getMaxAmount()));
            }
            if (request.getCurrency() != null && !request.getCurrency().isBlank()) {
                predicates.add(cb.equal(root.get("currency"), request.getCurrency().toUpperCase()));
            }

            // ==================== Vendor Filters ====================
            if (request.getVendorNames() != null && !request.getVendorNames().isEmpty()) {
                List<Predicate> vendorPredicates = request.getVendorNames().stream()
                        .map(v -> cb.like(cb.lower(root.get("vendorName")), "%" + v.toLowerCase() + "%"))
                        .toList();
                predicates.add(cb.or(vendorPredicates.toArray(new Predicate[0])));
            }
            if (request.getVendorId() != null) {
                predicates.add(cb.equal(root.get("vendor").get("id"), request.getVendorId()));
            }

            // ==================== Accounting Filters ====================
            if (request.getGlAccount() != null && !request.getGlAccount().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("glAccount")), 
                        "%" + request.getGlAccount().toLowerCase() + "%"));
            }
            if (request.getCostCenter() != null && !request.getCostCenter().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("costCenter")), 
                        "%" + request.getCostCenter().toLowerCase() + "%"));
            }
            if (request.getProject() != null && !request.getProject().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("project")), 
                        "%" + request.getProject().toLowerCase() + "%"));
            }
            if (request.getItemCategory() != null && !request.getItemCategory().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("itemCategory")), 
                        "%" + request.getItemCategory().toLowerCase() + "%"));
            }

            // ==================== Processing Filters ====================
            if (Boolean.TRUE.equals(request.getOverdueOnly())) {
                predicates.add(cb.isNotNull(root.get("dueDate")));
                predicates.add(cb.lessThan(root.get("dueDate"), LocalDate.now()));
                predicates.add(cb.notEqual(root.get("status"), InvoiceStatus.SYNCED));
            }
            if (Boolean.TRUE.equals(request.getRequiresManualReview())) {
                predicates.add(cb.equal(root.get("requiresManualReview"), true));
            }
            if (request.getMinConfidenceScore() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("confidenceScore"), request.getMinConfidenceScore()));
            }
            if (request.getMaxConfidenceScore() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("confidenceScore"), request.getMaxConfidenceScore()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
