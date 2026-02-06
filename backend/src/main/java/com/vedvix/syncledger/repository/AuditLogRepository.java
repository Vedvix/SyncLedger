package com.vedvix.syncledger.repository;

import com.vedvix.syncledger.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for AuditLog entity operations.
 * 
 * @author vedvix
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Find audit logs by user.
     */
    Page<AuditLog> findByUserId(Long userId, Pageable pageable);

    /**
     * Find audit logs by user email.
     */
    Page<AuditLog> findByUserEmail(String userEmail, Pageable pageable);

    /**
     * Find audit logs by action.
     */
    Page<AuditLog> findByAction(String action, Pageable pageable);

    /**
     * Find audit logs by entity.
     */
    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId, Pageable pageable);

    /**
     * Find audit logs by entity type.
     */
    Page<AuditLog> findByEntityType(String entityType, Pageable pageable);

    /**
     * Find audit logs in date range.
     */
    @Query("SELECT a FROM AuditLog a WHERE a.createdAt BETWEEN :start AND :end ORDER BY a.createdAt DESC")
    Page<AuditLog> findAuditLogsBetween(@Param("start") LocalDateTime start, 
                                         @Param("end") LocalDateTime end, 
                                         Pageable pageable);

    /**
     * Search audit logs.
     */
    @Query("SELECT a FROM AuditLog a WHERE " +
           "LOWER(a.action) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.userEmail) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<AuditLog> searchAuditLogs(@Param("query") String query, Pageable pageable);

    /**
     * Get action statistics.
     */
    @Query("SELECT a.action, COUNT(a) FROM AuditLog a GROUP BY a.action ORDER BY COUNT(a) DESC")
    List<Object[]> getActionStatistics();

    /**
     * Get user activity statistics.
     */
    @Query("SELECT a.userEmail, COUNT(a) FROM AuditLog a " +
           "WHERE a.createdAt >= :since GROUP BY a.userEmail ORDER BY COUNT(a) DESC")
    List<Object[]> getUserActivitySince(@Param("since") LocalDateTime since, Pageable pageable);

    /**
     * Find recent audit logs.
     */
    List<AuditLog> findTop50ByOrderByCreatedAtDesc();

    /**
     * Delete old audit logs.
     */
    @Query("DELETE FROM AuditLog a WHERE a.createdAt < :before")
    void deleteAuditLogsBefore(@Param("before") LocalDateTime before);
}
