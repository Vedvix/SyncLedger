package com.vedvix.syncledger.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Approval entity for tracking invoice approval workflow.
 * 
 * @author vedvix
 */
@Entity
@Table(name = "approvals", indexes = {
    @Index(name = "idx_approval_invoice", columnList = "invoice_id"),
    @Index(name = "idx_approval_user", columnList = "approver_id"),
    @Index(name = "idx_approval_action", columnList = "action")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Approval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id", nullable = false)
    private User approver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApprovalAction action;

    @Column(length = 1000)
    private String comments;

    @Column(length = 500)
    private String rejectionReason;

    @Column
    private Integer approvalLevel;

    @Column(length = 50)
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Check if this is an approval action.
     */
    public boolean isApproved() {
        return action == ApprovalAction.APPROVED;
    }

    /**
     * Check if this is a rejection action.
     */
    public boolean isRejected() {
        return action == ApprovalAction.REJECTED;
    }
}
