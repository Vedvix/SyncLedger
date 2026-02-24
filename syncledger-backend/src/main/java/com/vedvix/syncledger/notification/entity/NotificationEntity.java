package com.vedvix.syncledger.notification.entity;

import com.vedvix.syncledger.notification.domain.Recipient;
import com.vedvix.syncledger.notification.enums.NotificationChannel;
import com.vedvix.syncledger.notification.enums.NotificationPriority;
import com.vedvix.syncledger.notification.enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_status", columnList = "status"),
        @Index(name = "idx_notifications_tenant_created", columnList = "tenant_id,created_at"),
        @Index(name = "idx_notifications_scheduled", columnList = "scheduled_at"),
        @Index(name = "idx_notifications_channel", columnList = "channel")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEntity {

    @Id
    @Column(name = "notification_id", length = 100)
    private String notificationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(name = "template_name", nullable = false, length = 100)
    private String templateName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recipients_json", nullable = false)
    private List<Recipient> recipients;

    @Column(name = "parameters_json", columnDefinition = "TEXT")
    private String parametersJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationPriority priority;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "attempt_count", columnDefinition = "integer default 0")
    private int attemptCount;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "provider_id", length = 50)
    private String providerId;

    @Column(name = "external_id", length = 255)
    private String externalId;
}