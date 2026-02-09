package com.vedvix.syncledger.repository;

import com.vedvix.syncledger.model.EmailLog;
import com.vedvix.syncledger.model.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for EmailLog entity operations.
 * Supports multi-tenant queries scoped to organization.
 * 
 * @author vedvix
 */
@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    /**
     * Find email by message ID.
     */
    Optional<EmailLog> findByMessageId(String messageId);

    /**
     * Check if email has been processed.
     */
    boolean existsByMessageId(String messageId);

    /**
     * Find unprocessed emails for an organization.
     */
    List<EmailLog> findByOrganizationAndIsProcessedFalseOrderByReceivedAtAsc(Organization organization);

    /**
     * Find unprocessed emails (all organizations).
     */
    List<EmailLog> findByIsProcessedFalseOrderByReceivedAtAsc();

    /**
     * Find emails with errors for an organization.
     */
    Page<EmailLog> findByOrganizationAndHasErrorTrue(Organization organization, Pageable pageable);

    /**
     * Find emails with errors.
     */
    Page<EmailLog> findByHasErrorTrue(Pageable pageable);

    /**
     * Find processed emails for an organization.
     */
    Page<EmailLog> findByOrganizationAndIsProcessedTrue(Organization organization, Pageable pageable);

    /**
     * Find processed emails.
     */
    Page<EmailLog> findByIsProcessedTrue(Pageable pageable);

    /**
     * Find emails by sender for an organization.
     */
    Page<EmailLog> findByOrganizationAndFromAddressContainingIgnoreCase(
            Organization organization, String fromAddress, Pageable pageable);

    /**
     * Find emails by sender.
     */
    Page<EmailLog> findByFromAddressContainingIgnoreCase(String fromAddress, Pageable pageable);

    /**
     * Find emails in date range for an organization.
     */
    @Query("SELECT e FROM EmailLog e WHERE e.organization = :org AND e.receivedAt BETWEEN :start AND :end")
    Page<EmailLog> findByOrganizationAndReceivedBetween(
            @Param("org") Organization organization,
            @Param("start") LocalDateTime start, 
            @Param("end") LocalDateTime end, 
            Pageable pageable);

    /**
     * Find emails in date range.
     */
    @Query("SELECT e FROM EmailLog e WHERE e.receivedAt BETWEEN :start AND :end")
    Page<EmailLog> findEmailsReceivedBetween(@Param("start") LocalDateTime start, 
                                              @Param("end") LocalDateTime end, 
                                              Pageable pageable);

    /**
     * Count unprocessed emails for an organization.
     */
    long countByOrganizationAndIsProcessedFalse(Organization organization);

    /**
     * Count unprocessed emails.
     */
    long countByIsProcessedFalse();

    /**
     * Count emails with errors for an organization.
     */
    long countByOrganizationAndHasErrorTrue(Organization organization);

    /**
     * Count emails with errors.
     */
    long countByHasErrorTrue();

    /**
     * Get email processing statistics for an organization.
     */
    @Query("SELECT " +
           "COUNT(e), " +
           "SUM(CASE WHEN e.isProcessed = true THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN e.hasError = true THEN 1 ELSE 0 END), " +
           "SUM(COALESCE(e.invoicesExtracted, 0)) " +
           "FROM EmailLog e WHERE e.organization = :org")
    Object[] getEmailProcessingStatsByOrganization(@Param("org") Organization organization);

    /**
     * Get email processing statistics.
     */
    @Query("SELECT " +
           "COUNT(e), " +
           "SUM(CASE WHEN e.isProcessed = true THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN e.hasError = true THEN 1 ELSE 0 END), " +
           "SUM(COALESCE(e.invoicesExtracted, 0)) " +
           "FROM EmailLog e")
    Object[] getEmailProcessingStats();

    /**
     * Find recent emails for an organization.
     */
    List<EmailLog> findTop20ByOrganizationOrderByReceivedAtDesc(Organization organization);

    /**
     * Find recent emails.
     */
    List<EmailLog> findTop20ByOrderByReceivedAtDesc();

    /**
     * Find emails with attachments for an organization.
     */
    Page<EmailLog> findByOrganizationAndHasAttachmentsTrue(Organization organization, Pageable pageable);

    /**
     * Find emails with attachments.
     */
    Page<EmailLog> findByHasAttachmentsTrue(Pageable pageable);

    /**
     * Find all emails for an organization with pagination.
     */
    Page<EmailLog> findByOrganization(Organization organization, Pageable pageable);
}
