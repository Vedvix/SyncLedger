package com.vedvix.syncledger.notification.service;

import com.vedvix.syncledger.notification.domain.NotificationRequest;
import com.vedvix.syncledger.notification.dtos.NotificationResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface NotificationService {
    NotificationRequest sendNotification(NotificationRequest request);
    List<NotificationRequest> sendBatchNotifications(List<NotificationRequest> requests);
    Optional<NotificationResponseDto> getNotificationById(String id);
    Page<NotificationResponseDto> getNotificationsByTenant(String tenantId, Pageable pageable);
    void cancelNotification(String id);
}