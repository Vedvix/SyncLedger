package com.vedvix.syncledger.repository;

import com.vedvix.syncledger.model.SageSync;
import com.vedvix.syncledger.model.SyncStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for SageSync entity operations.
 * 
 * @author vedvix
 */
@Repository
public interface SageSyncRepository extends JpaRepository<SageSync, Long> {

    /**
     * Find sync logs for an invoice.
     */
    List<SageSync> findByInvoiceIdOrderByCreatedAtDesc(Long invoiceId);

    /**
     * Find sync logs by status.
     */
    Page<SageSync> findByStatus(SyncStatus status, Pageable pageable);

    /**
     * Find failed syncs.
     */
    List<SageSync> findByStatusAndAttemptNumberLessThan(SyncStatus status, Integer maxAttempts);

    /**
     * Get latest sync for an invoice.
     */
    @Query("SELECT s FROM SageSync s WHERE s.invoice.id = :invoiceId ORDER BY s.createdAt DESC LIMIT 1")
    SageSync findLatestSyncForInvoice(@Param("invoiceId") Long invoiceId);

    /**
     * Find syncs in date range.
     */
    @Query("SELECT s FROM SageSync s WHERE s.createdAt BETWEEN :start AND :end")
    Page<SageSync> findSyncsBetween(@Param("start") LocalDateTime start, 
                                     @Param("end") LocalDateTime end, 
                                     Pageable pageable);

    /**
     * Count syncs by status.
     */
    long countByStatus(SyncStatus status);

    /**
     * Get sync statistics.
     */
    @Query("SELECT s.status, COUNT(s), AVG(s.durationMs) FROM SageSync s GROUP BY s.status")
    List<Object[]> getSyncStatsByStatus();

    /**
     * Find recent failed syncs.
     */
    List<SageSync> findTop10ByStatusOrderByCreatedAtDesc(SyncStatus status);

    /**
     * Get average sync duration.
     */
    @Query("SELECT AVG(s.durationMs) FROM SageSync s WHERE s.status = 'SUCCESS'")
    Double getAverageSuccessfulSyncDuration();
}
