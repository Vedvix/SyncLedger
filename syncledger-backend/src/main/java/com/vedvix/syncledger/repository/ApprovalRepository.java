package com.vedvix.syncledger.repository;

import com.vedvix.syncledger.model.Approval;
import com.vedvix.syncledger.model.ApprovalAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Approval entity operations.
 * 
 * @author vedvix
 */
@Repository
public interface ApprovalRepository extends JpaRepository<Approval, Long> {

    /**
     * Find approvals for an invoice.
     */
    List<Approval> findByInvoiceIdOrderByCreatedAtDesc(Long invoiceId);

    /**
     * Find approvals by approver.
     */
    Page<Approval> findByApproverId(Long approverId, Pageable pageable);

    /**
     * Find approvals by action.
     */
    List<Approval> findByAction(ApprovalAction action);

    /**
     * Find approvals by approver and action.
     */
    Page<Approval> findByApproverIdAndAction(Long approverId, ApprovalAction action, Pageable pageable);

    /**
     * Find recent approvals by approver.
     */
    List<Approval> findTop10ByApproverIdOrderByCreatedAtDesc(Long approverId);

    /**
     * Count approvals by action for a user.
     */
    long countByApproverIdAndAction(Long approverId, ApprovalAction action);

    /**
     * Find approvals in date range.
     */
    @Query("SELECT a FROM Approval a WHERE a.createdAt BETWEEN :start AND :end")
    List<Approval> findApprovalsBetween(@Param("start") LocalDateTime start, 
                                         @Param("end") LocalDateTime end);

    /**
     * Get approval statistics by user.
     */
    @Query("SELECT a.approver.id, a.approver.email, a.action, COUNT(a) " +
           "FROM Approval a GROUP BY a.approver.id, a.approver.email, a.action")
    List<Object[]> getApprovalStatsByUser();

    /**
     * Check if user has already approved/rejected an invoice.
     */
    boolean existsByInvoiceIdAndApproverId(Long invoiceId, Long approverId);

    /**
     * Get latest approval for an invoice.
     */
    @Query("SELECT a FROM Approval a WHERE a.invoice.id = :invoiceId ORDER BY a.createdAt DESC LIMIT 1")
    Approval findLatestApprovalForInvoice(@Param("invoiceId") Long invoiceId);
}
