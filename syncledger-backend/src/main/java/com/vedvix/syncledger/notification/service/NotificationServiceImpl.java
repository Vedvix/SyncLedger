package com.vedvix.syncledger.notification.service;

import com.vedvix.syncledger.notification.domain.NotificationRequest;
import com.vedvix.syncledger.notification.domain.NotificationResult;
import com.vedvix.syncledger.notification.dtos.NotificationResponseDto;
import com.vedvix.syncledger.notification.entity.NotificationEntity;
import com.vedvix.syncledger.notification.enums.NotificationChannel;
import com.vedvix.syncledger.notification.enums.NotificationStatus;
import com.vedvix.syncledger.notification.mapper.NotificationMapper;
import com.vedvix.syncledger.notification.provider.NotificationProvider;
import com.vedvix.syncledger.notification.provider.email.EmailProvider;
import com.vedvix.syncledger.notification.provider.push.PushProvider;
import com.vedvix.syncledger.notification.provider.sms.SmsProvider;
import com.vedvix.syncledger.notification.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Notification service implementation.
 * Persists notification records and dispatches to the appropriate provider
 * (SES for email, Twilio/SMSOzone for SMS, FCM for push).
 * If no provider is enabled for the requested channel, the notification is
 * still persisted as a record (useful for audit/history) with status SENT.
 */
@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository repository;
    private final NotificationMapper mapper;
    private final List<EmailProvider> emailProviders;
    private final List<SmsProvider> smsProviders;
    private final List<PushProvider> pushProviders;

    public NotificationServiceImpl(NotificationRepository repository,
                                   NotificationMapper mapper,
                                   List<EmailProvider> emailProviders,
                                   List<SmsProvider> smsProviders,
                                   List<PushProvider> pushProviders) {
        this.repository = repository;
        this.mapper = mapper;
        this.emailProviders = emailProviders != null ? emailProviders : List.of();
        this.smsProviders = smsProviders != null ? smsProviders : List.of();
        this.pushProviders = pushProviders != null ? pushProviders : List.of();
    }

    @Override
    @Transactional
    public NotificationRequest sendNotification(NotificationRequest request) {
        log.info("Processing notification request: channel={}, template={}",
                request.channel(), request.templateName());

        // Persist notification record
        NotificationEntity entity = mapper.toEntity(request);
        entity.setStatus(NotificationStatus.PENDING);
        entity = repository.save(entity);

        log.info("Notification persisted id={}, channel={}, template={}, recipients={}",
                entity.getNotificationId(), request.channel(), request.templateName(),
                request.recipients() != null ? request.recipients().size() : 0);

        // Dispatch to the appropriate provider
        try {
            NotificationProvider provider = resolveProvider(request.channel());
            if (provider != null) {
                NotificationResult result = provider.send(request);
                entity.setStatus(result.status());
                entity.setProviderId(result.providerId());
                entity.setExternalId(result.externalId());
                entity.setSentAt(result.sentAt() != null ? result.sentAt() : Instant.now());
                entity.setAttemptCount(result.attemptCount());
                log.info("Notification dispatched via provider={}, externalId={}",
                        result.providerId(), result.externalId());
            } else {
                // No provider available — mark as SENT (record-only mode)
                entity.setStatus(NotificationStatus.SENT);
                entity.setSentAt(Instant.now());
                log.info("No provider enabled for channel={}, notification recorded only", request.channel());
            }
        } catch (Exception e) {
            entity.setStatus(NotificationStatus.FAILED);
            entity.setErrorMessage(e.getMessage());
            entity.setAttemptCount(entity.getAttemptCount() + 1);
            log.error("Failed to dispatch notification id={}: {}", entity.getNotificationId(), e.getMessage());
        }

        entity = repository.save(entity);
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

    /**
     * Resolve the best enabled provider for a given channel.
     * Returns the highest-priority (lowest number) enabled provider, or null.
     */
    private NotificationProvider resolveProvider(NotificationChannel channel) {
        List<? extends NotificationProvider> candidates = switch (channel) {
            case EMAIL -> emailProviders;
            case SMS -> smsProviders;
            case PUSH -> pushProviders;
        };

        return candidates.stream()
                .filter(NotificationProvider::isEnabled)
                .min(Comparator.comparingInt(NotificationProvider::getPriority))
                .orElse(null);
    }
}