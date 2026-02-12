package com.vedvix.syncledger.repository;

import com.vedvix.syncledger.model.Vendor;
import com.vedvix.syncledger.model.VendorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Vendor entity with analytics queries.
 * 
 * @author vedvix
 */
@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long>, JpaSpecificationExecutor<Vendor> {

    // ─── Basic Finders (org-scoped) ──────────────────────────────────────

    Page<Vendor> findByOrganization_Id(Long orgId, Pageable pageable);

    Optional<Vendor> findByIdAndOrganization_Id(Long id, Long orgId);

    Optional<Vendor> findByOrganization_IdAndNormalizedName(Long orgId, String normalizedName);

    List<Vendor> findByOrganization_IdAndStatus(Long orgId, VendorStatus status);

    long countByOrganization_Id(Long orgId);

    long countByOrganization_IdAndStatus(Long orgId, VendorStatus status);

    // ─── Search ──────────────────────────────────────────────────────────

    @Query("SELECT v FROM Vendor v WHERE v.organization.id = :orgId " +
           "AND (LOWER(v.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(v.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(v.code) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(v.taxId) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Vendor> searchVendorsInOrganization(@Param("orgId") Long orgId, 
                                              @Param("search") String search, 
                                              Pageable pageable);

    @Query("SELECT v FROM Vendor v WHERE " +
           "LOWER(v.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(v.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(v.code) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Vendor> searchAllVendors(@Param("search") String search, Pageable pageable);

    // ─── Analytics: Per-vendor invoice metrics ───────────────────────────

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.vendor.id = :vendorId")
    Long countInvoicesByVendor(@Param("vendorId") Long vendorId);

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.vendor.id = :vendorId")
    BigDecimal sumTotalAmountByVendor(@Param("vendorId") Long vendorId);

    @Query("SELECT COALESCE(AVG(i.totalAmount), 0) FROM Invoice i WHERE i.vendor.id = :vendorId")
    BigDecimal avgTotalAmountByVendor(@Param("vendorId") Long vendorId);

    @Query("SELECT COALESCE(MIN(i.totalAmount), 0) FROM Invoice i WHERE i.vendor.id = :vendorId")
    BigDecimal minTotalAmountByVendor(@Param("vendorId") Long vendorId);

    @Query("SELECT COALESCE(MAX(i.totalAmount), 0) FROM Invoice i WHERE i.vendor.id = :vendorId")
    BigDecimal maxTotalAmountByVendor(@Param("vendorId") Long vendorId);

    @Query("SELECT COALESCE(SUM(i.taxAmount), 0) FROM Invoice i WHERE i.vendor.id = :vendorId")
    BigDecimal sumTaxAmountByVendor(@Param("vendorId") Long vendorId);

    @Query("SELECT COALESCE(AVG(i.confidenceScore), 0) FROM Invoice i WHERE i.vendor.id = :vendorId AND i.confidenceScore IS NOT NULL")
    BigDecimal avgConfidenceByVendor(@Param("vendorId") Long vendorId);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.vendor.id = :vendorId AND i.status = :status")
    Long countInvoicesByVendorAndStatus(@Param("vendorId") Long vendorId, 
                                         @Param("status") com.vedvix.syncledger.model.InvoiceStatus status);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.vendor.id = :vendorId AND i.requiresManualReview = true")
    Long countInvoicesRequiringReviewByVendor(@Param("vendorId") Long vendorId);

    @Query("SELECT MIN(i.invoiceDate) FROM Invoice i WHERE i.vendor.id = :vendorId")
    LocalDate findFirstInvoiceDateByVendor(@Param("vendorId") Long vendorId);

    @Query("SELECT MAX(i.invoiceDate) FROM Invoice i WHERE i.vendor.id = :vendorId")
    LocalDate findLastInvoiceDateByVendor(@Param("vendorId") Long vendorId);

    // ─── Analytics: Organization-wide vendor summary ─────────────────────

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.vendor IS NOT NULL AND i.vendor.organization.id = :orgId")
    BigDecimal sumTotalAmountAcrossVendors(@Param("orgId") Long orgId);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.vendor IS NOT NULL AND i.vendor.organization.id = :orgId")
    Long countInvoicesAcrossVendors(@Param("orgId") Long orgId);

    // ─── Top vendors (native query for GROUP BY) ─────────────────────────

    @Query(value = "SELECT v.id AS vendorId, v.name AS vendorName, " +
                   "COUNT(i.id) AS invoiceCount, " +
                   "COALESCE(SUM(i.total_amount), 0) AS totalAmount, " +
                   "COALESCE(AVG(i.total_amount), 0) AS averageAmount " +
                   "FROM vendors v LEFT JOIN invoices i ON i.vendor_id = v.id " +
                   "WHERE v.organization_id = :orgId " +
                   "GROUP BY v.id, v.name " +
                   "ORDER BY invoiceCount DESC " +
                   "LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopVendorsByInvoiceCount(@Param("orgId") Long orgId, @Param("limit") int limit);

    @Query(value = "SELECT v.id AS vendorId, v.name AS vendorName, " +
                   "COUNT(i.id) AS invoiceCount, " +
                   "COALESCE(SUM(i.total_amount), 0) AS totalAmount, " +
                   "COALESCE(AVG(i.total_amount), 0) AS averageAmount " +
                   "FROM vendors v LEFT JOIN invoices i ON i.vendor_id = v.id " +
                   "WHERE v.organization_id = :orgId " +
                   "GROUP BY v.id, v.name " +
                   "ORDER BY totalAmount DESC " +
                   "LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopVendorsByTotalAmount(@Param("orgId") Long orgId, @Param("limit") int limit);

    // ─── Monthly totals for a vendor ─────────────────────────────────────

    @Query(value = "SELECT TO_CHAR(i.invoice_date, 'YYYY-MM') AS month, " +
                   "COALESCE(SUM(i.total_amount), 0) AS total " +
                   "FROM invoices i " +
                   "WHERE i.vendor_id = :vendorId " +
                   "AND i.invoice_date >= :startDate " +
                   "GROUP BY TO_CHAR(i.invoice_date, 'YYYY-MM') " +
                   "ORDER BY month", nativeQuery = true)
    List<Object[]> findMonthlyTotalsByVendor(@Param("vendorId") Long vendorId, 
                                              @Param("startDate") LocalDate startDate);

    // ─── Global queries (Super Admin) ────────────────────────────────────

    @Query("SELECT v FROM Vendor v WHERE v.status = :status")
    Page<Vendor> findByStatus(@Param("status") VendorStatus status, Pageable pageable);

    boolean existsByOrganization_IdAndNormalizedName(Long orgId, String normalizedName);
}
