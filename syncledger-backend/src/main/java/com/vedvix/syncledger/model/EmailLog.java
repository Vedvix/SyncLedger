package com.vedvix.syncledger.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Email log entity for tracking email processing.
 * Each email is associated with an organization (multi-tenant isolation).
 * 
 * @author vedvix
 */
@Entity
@Table(name = "email_logs", indexes = {
    @Index(name = "idx_email_log_message_id", columnList = "messageId"),
    @Index(name = "idx_email_log_org_id", columnList = "organization_id"),
    @Index(name = "idx_email_log_from", columnList = "fromAddress"),
    @Index(name = "idx_email_log_processed", columnList = "isProcessed"),
    @Index(name = "idx_email_log_received", columnList = "receivedAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Multi-tenant: Associate email log with organization
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, unique = true, length = 500)
    private String messageId;

    @Column(length = 500)
    private String internetMessageId;

    @Column(length = 255)
    private String fromAddress;

    @Column(length = 255)
    private String fromName;

    @Column(length = 1000)
    private String toAddresses;

    @Column(length = 500)
    private String subject;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String bodyPreview;

    @Column(nullable = false)
    private LocalDateTime receivedAt;

    @Column
    @Builder.Default
    private Boolean hasAttachments = false;

    @Column
    private Integer attachmentCount;

    @Column(length = 1000)
    private String attachmentNames;

    @Column
    @Builder.Default
    private Boolean isProcessed = false;

    @Column
    private LocalDateTime processedAt;

    @Column
    private Integer invoicesExtracted;

    @Column
    @Builder.Default
    private Boolean hasError = false;

    @Column(length = 1000)
    private String errorMessage;

    @Column(name = "processing_duration_ms")
    private Long processingDurationMs;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Mark email as processed.
     */
    public void markProcessed(int invoiceCount) {
        this.isProcessed = true;
        this.processedAt = LocalDateTime.now();
        this.invoicesExtracted = invoiceCount;
    }

    /**
     * Mark email processing as failed.
     */
    public void markFailed(String error) {
        this.hasError = true;
        this.errorMessage = error;
        this.processedAt = LocalDateTime.now();
    }
}
