package com.vedvix.syncledger.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Audit trail entry for tracking invoice lifecycle events.
 * Records every significant action from receipt through ERP synchronization.
 *
 * @author vedvix
 */
@Entity
@Table(name = "invoice_audit_events", indexes = {
    @Index(name = "idx_iae_invoice_id", columnList = "invoice_id"),
    @Index(name = "idx_iae_org_id", columnList = "organization_id"),
    @Index(name = "idx_iae_event_type", columnList = "event_type"),
    @Index(name = "idx_iae_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The invoice this event belongs to.
     */
    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    /**
     * Organization scope (for multi-tenant queries).
     */
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    /**
     * Type of lifecycle event.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private InvoiceAuditEventType eventType;

    /**
     * Previous invoice status (null for initial events).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private InvoiceStatus fromStatus;

    /**
     * New invoice status after the event (null if status unchanged).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 30)
    private InvoiceStatus toStatus;

    /**
     * ID of the user who performed this action (null for system events).
     */
    @Column(name = "performed_by_user_id")
    private Long performedByUserId;

    /**
     * Display name of the performer (denormalized for timeline display).
     */
    @Column(name = "performed_by_name", length = 200)
    private String performedByName;

    /**
     * Human-readable description of the event.
     */
    @Column(name = "description", length = 1000)
    private String description;

    /**
     * Additional structured data (JSON).
     * E.g., changed fields, confidence score, error message, sync ID, etc.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    /**
     * IP address of the client (for manual actions).
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * Timestamp of the event.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
