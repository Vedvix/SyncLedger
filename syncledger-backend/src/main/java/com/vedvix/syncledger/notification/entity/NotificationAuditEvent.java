package com.vedvix.syncledger.notification.entity;

import com.vedvix.syncledger.notification.enums.NotificationChannel;
import com.vedvix.syncledger.notification.enums.NotificationEventType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "notification_audit_events",
        indexes = {
                @Index(name = "idx_audit_notification_id", columnList = "notification_id"),
                @Index(name = "idx_audit_tenant_timestamp", columnList = "tenant_id, created_at DESC"),
                @Index(name = "idx_audit_event_type", columnList = "event_type"),
                @Index(name = "idx_audit_timestamp", columnList = "created_at DESC")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long eventId;

    // FK → notifications.notification_id (VARCHAR)
    @Column(name = "notification_id", length = 100, nullable = false)
    private String notificationId;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private NotificationEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 20)
    private NotificationChannel channel;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    // FK → users.user_id
    @Column(name = "user_id")
    private Long userId;

    // JSONB
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event_data", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> eventData;

    // PostgreSQL INET → String (recommended)
//    @Column(name = "ip_address")
//    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
