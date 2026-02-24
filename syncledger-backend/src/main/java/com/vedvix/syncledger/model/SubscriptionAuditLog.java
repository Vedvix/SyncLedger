package com.vedvix.syncledger.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Audit log for subscription lifecycle events.
 * Immutable record for compliance and debugging.
 * 
 * @author vedvix
 */
@Entity
@Table(name = "subscription_audit_logs", indexes = {
    @Index(name = "idx_sub_audit_org", columnList = "organization_id"),
    @Index(name = "idx_sub_audit_event", columnList = "eventType")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "subscription_id")
    private Long subscriptionId;

    @Column(nullable = false, length = 50)
    private String eventType;

    @Column(length = 30)
    private String oldStatus;

    @Column(length = 30)
    private String newStatus;

    @Column(length = 30)
    private String oldPlan;

    @Column(length = 30)
    private String newPlan;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "performed_by")
    private Long performedBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
