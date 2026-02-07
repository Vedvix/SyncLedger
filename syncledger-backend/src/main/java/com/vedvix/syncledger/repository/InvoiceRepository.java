package com.vedvix.syncledger.repository;

import com.vedvix.syncledger.model.Invoice;
import com.vedvix.syncledger.model.InvoiceStatus;
import com.vedvix.syncledger.model.SyncStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Invoice entity operations.
 * Supports multi-tenant queries with organization filtering.
 * 
 * @author vedvix
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {

    // ==================== Organization-scoped queries ====================

    /**
     * Find invoice by ID and organization.
     */
    Optional<Invoice> findByIdAndOrganization_Id(Long id, Long organizationId);

    /**
     * Find invoices by organization.
     */
    Page<Invoice> findByOrganization_Id(Long organizationId, Pageable pageable);

    /**
     * Find invoices by organization and status.
     */
    Page<Invoice> findByOrganization_IdAndStatus(Long organizationId, InvoiceStatus status, Pageable pageable);

    /**
     * Find invoices by organization and multiple statuses.
     */
    Page<Invoice> findByOrganization_IdAndStatusIn(Long organizationId, List<InvoiceStatus> statuses, Pageable pageable);

    /**
     * Search invoices within organization.
     */
    @Query("SELECT i FROM Invoice i WHERE i.organization.id = :orgId AND (" +
           "LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(i.vendorName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(i.poNumber) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Invoice> searchInvoicesInOrganization(@Param("orgId") Long orgId, 
                                                @Param("query") String query, 
                                                Pageable pageable);

    /**
     * Count invoices by organization and status.
     */
    long countByOrganization_IdAndStatus(Long organizationId, InvoiceStatus status);

    /**
     * Count all invoices by organization.
     */
    long countByOrganization_Id(Long organizationId);

    /**
     * Get total amount by organization and status.
     */
    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.organization.id = :orgId AND i.status = :status")
    BigDecimal sumTotalAmountByOrganizationAndStatus(@Param("orgId") Long orgId, @Param("status") InvoiceStatus status);

    /**
     * Get dashboard statistics for organization.
     */
    @Query("SELECT i.status, COUNT(i), COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.organization.id = :orgId GROUP BY i.status")
    List<Object[]> getInvoiceStatsByStatusForOrganization(@Param("orgId") Long orgId);

    /**
     * Find overdue invoices for organization.
     */
    @Query("SELECT i FROM Invoice i WHERE i.organization.id = :orgId AND i.dueDate < :today AND i.status NOT IN ('SYNCED', 'ARCHIVED')")
    List<Invoice> findOverdueInvoicesForOrganization(@Param("orgId") Long orgId, @Param("today") LocalDate today);

    /**
     * Find invoices pending sync for organization.
     */
    @Query("SELECT i FROM Invoice i WHERE i.organization.id = :orgId AND i.status = 'APPROVED' AND (i.syncStatus IS NULL OR i.syncStatus = 'FAILED')")
    List<Invoice> findInvoicesPendingSyncForOrganization(@Param("orgId") Long orgId);

    /**
     * Find invoices by date range for organization.
     */
    @Query("SELECT i FROM Invoice i WHERE i.organization.id = :orgId AND i.invoiceDate BETWEEN :startDate AND :endDate")
    Page<Invoice> findByOrganizationAndInvoiceDateBetween(@Param("orgId") Long orgId,
                                                          @Param("startDate") LocalDate startDate, 
                                                          @Param("endDate") LocalDate endDate, 
                                                          Pageable pageable);

    /**
     * Get top vendors by invoice count for organization.
     */
    @Query("SELECT i.vendorName, COUNT(i), SUM(i.totalAmount) FROM Invoice i " +
           "WHERE i.organization.id = :orgId GROUP BY i.vendorName ORDER BY COUNT(i) DESC")
    List<Object[]> getTopVendorsByInvoiceCountForOrganization(@Param("orgId") Long orgId, Pageable pageable);

    /**
     * Find recent invoices for organization.
     */
    List<Invoice> findTop10ByOrganization_IdOrderByCreatedAtDesc(Long organizationId);

    // ==================== Global queries (for Super Admin) ====================

    /**
     * Find invoice by invoice number.
     */
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    /**
     * Check if invoice number exists.
     */
    boolean existsByInvoiceNumber(String invoiceNumber);

    /**
     * Find invoices by status.
     */
    Page<Invoice> findByStatus(InvoiceStatus status, Pageable pageable);

    /**
     * Find invoices by multiple statuses.
     */
    Page<Invoice> findByStatusIn(List<InvoiceStatus> statuses, Pageable pageable);

    /**
     * Find invoices by vendor name.
     */
    Page<Invoice> findByVendorNameContainingIgnoreCase(String vendorName, Pageable pageable);

    /**
     * Find invoices requiring manual review.
     */
    Page<Invoice> findByRequiresManualReviewTrue(Pageable pageable);

    /**
     * Find invoices assigned to a user.
     */
    Page<Invoice> findByAssignedToId(Long userId, Pageable pageable);

    /**
     * Find invoices by date range.
     */
    @Query("SELECT i FROM Invoice i WHERE i.invoiceDate BETWEEN :startDate AND :endDate")
    Page<Invoice> findByInvoiceDateBetween(@Param("startDate") LocalDate startDate, 
                                            @Param("endDate") LocalDate endDate, 
                                            Pageable pageable);

    /**
     * Find overdue invoices.
     */
    @Query("SELECT i FROM Invoice i WHERE i.dueDate < :today AND i.status NOT IN ('SYNCED', 'ARCHIVED')")
    List<Invoice> findOverdueInvoices(@Param("today") LocalDate today);

    /**
     * Find invoices pending sync.
     */
    @Query("SELECT i FROM Invoice i WHERE i.status = 'APPROVED' AND (i.syncStatus IS NULL OR i.syncStatus = 'FAILED')")
    List<Invoice> findInvoicesPendingSync();

    /**
     * Find invoices by sync status.
     */
    List<Invoice> findBySyncStatus(SyncStatus syncStatus);

    /**
     * Search invoices by various criteria.
     */
    @Query("SELECT i FROM Invoice i WHERE " +
           "LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(i.vendorName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(i.poNumber) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Invoice> searchInvoices(@Param("query") String query, Pageable pageable);

    /**
     * Count invoices by status.
     */
    long countByStatus(InvoiceStatus status);

    /**
     * Get total amount by status.
     */
    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.status = :status")
    BigDecimal sumTotalAmountByStatus(@Param("status") InvoiceStatus status);

    /**
     * Get dashboard statistics.
     */
    @Query("SELECT i.status, COUNT(i), COALESCE(SUM(i.totalAmount), 0) FROM Invoice i GROUP BY i.status")
    List<Object[]> getInvoiceStatsByStatus();

    /**
     * Find invoices created in date range.
     */
    @Query("SELECT i FROM Invoice i WHERE i.createdAt BETWEEN :start AND :end")
    List<Invoice> findInvoicesCreatedBetween(@Param("start") LocalDateTime start, 
                                              @Param("end") LocalDateTime end);

    /**
     * Get monthly invoice totals.
     */
    @Query(value = "SELECT EXTRACT(MONTH FROM i.invoice_date) as month, " +
                   "EXTRACT(YEAR FROM i.invoice_date) as year, " +
                   "COUNT(*) as count, COALESCE(SUM(i.total_amount), 0) as total " +
                   "FROM invoices i " +
                   "WHERE i.invoice_date >= :startDate " +
                   "GROUP BY EXTRACT(YEAR FROM i.invoice_date), EXTRACT(MONTH FROM i.invoice_date) " +
                   "ORDER BY year, month", nativeQuery = true)
    List<Object[]> getMonthlyInvoiceTotals(@Param("startDate") LocalDate startDate);

    /**
     * Get top vendors by invoice count.
     */
    @Query("SELECT i.vendorName, COUNT(i), SUM(i.totalAmount) FROM Invoice i " +
           "GROUP BY i.vendorName ORDER BY COUNT(i) DESC")
    List<Object[]> getTopVendorsByInvoiceCount(Pageable pageable);

    /**
     * Find invoices from email source.
     */
    List<Invoice> findBySourceEmailId(String emailId);

    /**
     * Find recent invoices.
     */
    List<Invoice> findTop10ByOrderByCreatedAtDesc();
}

