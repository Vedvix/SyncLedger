package com.vedvix.syncledger.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Audit log entity for tracking all system activities.
 * 
 * @author vedvix
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_user", columnList = "user_id"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_entity", columnList = "entityType, entityId"),
    @Index(name = "idx_audit_created", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 255)
    private String userEmail;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(length = 100)
    private String entityType;

    @Column
    private Long entityId;

    @Column(length = 500)
    private String description;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String oldValues;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String newValues;

    @Column(length = 50)
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    @Column(length = 100)
    private String sessionId;

    @Column(length = 500)
    private String requestUri;

    @Column(length = 10)
    private String requestMethod;

    @Column
    private Integer responseStatus;

    @Column
    private Long durationMs;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Create audit log for entity creation.
     */
    public static AuditLog createLog(User user, String entityType, Long entityId, String newValues) {
        return AuditLog.builder()
                .user(user)
                .userEmail(user != null ? user.getEmail() : null)
                .action("CREATE")
                .entityType(entityType)
                .entityId(entityId)
                .newValues(newValues)
                .build();
    }

    /**
     * Create audit log for entity update.
     */
    public static AuditLog updateLog(User user, String entityType, Long entityId, 
                                     String oldValues, String newValues) {
        return AuditLog.builder()
                .user(user)
                .userEmail(user != null ? user.getEmail() : null)
                .action("UPDATE")
                .entityType(entityType)
                .entityId(entityId)
                .oldValues(oldValues)
                .newValues(newValues)
                .build();
    }

    /**
     * Create audit log for entity deletion.
     */
    public static AuditLog deleteLog(User user, String entityType, Long entityId, String oldValues) {
        return AuditLog.builder()
                .user(user)
                .userEmail(user != null ? user.getEmail() : null)
                .action("DELETE")
                .entityType(entityType)
                .entityId(entityId)
                .oldValues(oldValues)
                .build();
    }
}
