package com.vedvix.syncledger.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Sage synchronization log entity for tracking sync operations.
 * 
 * @author vedvix
 */
@Entity
@Table(name = "sage_sync_logs", indexes = {
    @Index(name = "idx_sage_sync_invoice", columnList = "invoice_id"),
    @Index(name = "idx_sage_sync_status", columnList = "status"),
    @Index(name = "idx_sage_sync_created", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SageSync {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SyncStatus status;

    @Column(length = 100)
    private String sageInvoiceId;

    @Column(length = 100)
    private String sageTransactionId;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String requestPayload;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String responsePayload;

    @Column
    private Integer httpStatusCode;

    @Column(length = 1000)
    private String errorMessage;

    @Column(length = 100)
    private String errorCode;

    @Column
    private Integer attemptNumber;

    @Column
    private Integer durationMs;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triggered_by_id")
    private User triggeredBy;

    @Column(length = 50)
    private String triggerType;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Check if sync was successful.
     */
    public boolean isSuccess() {
        return status == SyncStatus.SUCCESS;
    }

    /**
     * Check if sync can be retried.
     */
    public boolean canRetry() {
        return status == SyncStatus.FAILED && attemptNumber < 3;
    }
}
