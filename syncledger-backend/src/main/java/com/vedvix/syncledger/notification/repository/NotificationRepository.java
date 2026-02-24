package com.vedvix.syncledger.notification.repository;

import com.vedvix.syncledger.notification.entity.NotificationEntity;
import com.vedvix.syncledger.notification.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, String> {

    List<NotificationEntity> findByStatus(NotificationStatus status);

    Page<NotificationEntity> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    @Query("SELECT n FROM NotificationEntity n WHERE n.scheduledAt <= :now AND n.status = 'PENDING'")
    List<NotificationEntity> findScheduledNotifications(@Param("now") Instant now);

    @Query("SELECT COUNT(n) FROM NotificationEntity n WHERE n.tenantId = :tenantId AND n.createdAt >= :since")
    long countByTenantIdSince(@Param("tenantId") String tenantId, @Param("since") Instant since);
}