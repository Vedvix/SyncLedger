package com.vedvix.syncledger.notification.service;

import com.vedvix.syncledger.notification.domain.NotificationRequest;
import com.vedvix.syncledger.notification.dtos.NotificationResponseDto;
import com.vedvix.syncledger.notification.entity.NotificationEntity;
import com.vedvix.syncledger.notification.enums.NotificationStatus;
import com.vedvix.syncledger.notification.mapper.NotificationMapper;
import com.vedvix.syncledger.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Notification service implementation.
 * Persists notification records and logs delivery.
 * In production, integrate an async message broker for queued delivery.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository repository;
    private final NotificationMapper mapper;

    @Override
    @Transactional
    public NotificationRequest sendNotification(NotificationRequest request) {
        log.info("Processing notification request: channel={}, template={}",
                request.channel(), request.templateName());

        // Persist notification record
        NotificationEntity entity = mapper.toEntity(request);
        entity.setStatus(NotificationStatus.SENT);
        entity = repository.save(entity);

        log.info("Notification persisted id={}, channel={}, template={}, recipients={}",
                entity.getNotificationId(), request.channel(), request.templateName(),
                request.recipients() != null ? request.recipients().size() : 0);

        return mapper.toDomain(entity);
    }

    @Override
    @Transactional
    public List<NotificationRequest> sendBatchNotifications(List<NotificationRequest> requests) {
        return requests.stream()
                .map(this::sendNotification)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NotificationResponseDto> getNotificationById(String id) {
        return repository.findById(id)
                .map(mapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponseDto> getNotificationsByTenant(String tenantId, Pageable pageable) {
        return repository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable)
                .map(mapper::toResponseDto);
    }

    @Override
    @Transactional
    public void cancelNotification(String id) {
        repository.findById(id).ifPresent(entity -> {
            if (entity.getStatus() == NotificationStatus.PENDING ||
                    entity.getStatus() == NotificationStatus.QUEUED) {
                entity.setStatus(NotificationStatus.CANCELLED);
                repository.save(entity);
                log.info("Cancelled notification: {}", id);
            }
        });
    }
}