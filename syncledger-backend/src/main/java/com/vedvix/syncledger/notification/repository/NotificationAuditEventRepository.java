package com.vedvix.syncledger.notification.repository;

import com.vedvix.syncledger.notification.entity.NotificationAuditEvent;
import com.vedvix.syncledger.notification.enums.NotificationEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface NotificationAuditEventRepository extends JpaRepository<NotificationAuditEvent, Long> {

    List<NotificationAuditEvent> findByNotificationIdOrderByCreatedAtDesc(String notificationId);

    List<NotificationAuditEvent> findByTenantIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            String tenantId,
            Instant start,
            Instant end
    );

    List<NotificationAuditEvent> findByEventTypeOrderByCreatedAtDesc(NotificationEventType eventType);
}