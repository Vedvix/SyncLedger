package com.vedvix.syncledger.notification.provider.push;

import com.google.firebase.messaging.*;
import com.vedvix.syncledger.notification.exception.ProviderException;
import com.vedvix.syncledger.notification.domain.NotificationRequest;
import com.vedvix.syncledger.notification.domain.NotificationResult;
import com.vedvix.syncledger.notification.domain.Recipient;
import com.vedvix.syncledger.notification.enums.NotificationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "notification.providers.push.fcm", name = "enabled", havingValue = "true")
public class FcmPushProvider implements PushProvider {

    private final FirebaseMessaging firebaseMessaging;
    private final FcmConfig config;

    @Override
    public NotificationResult send(NotificationRequest request) {
        log.info("Sending push notification {} using FCM", request.id());

        try {
            for (Recipient recipient : request.recipients()) {
                if (recipient.deviceToken() == null || recipient.deviceToken().isBlank()) {
                    log.warn("Skipping recipient without device token for notification {}", request.id());
                    continue;
                }

                Message message = buildMessage(request, recipient);

                String messageId = firebaseMessaging.send(message);

                log.info("Push notification sent successfully. Message ID: {}", messageId);

                return NotificationResult.builder()
                        .notificationId(request.id())
                        .recipientId(recipient.deviceToken())
                        .status(NotificationStatus.SENT)
                        .providerId(getProviderId())
                        .externalId(messageId)
                        .sentAt(Instant.now())
                        .attemptCount(1)
                        .build();
            }

            throw new ProviderException("NO_RECIPIENTS", "No valid recipients found", false);

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send push notification via FCM: {}", e.getMessage(), e);

            boolean retryable = isRetryableError(e);
            throw new ProviderException(
                    "FCM_ERROR",
                    "Failed to send push notification: " + e.getMessage(),
                    e,
                    retryable
            );
        }
    }

    @Override
    public String getProviderId() {
        return "fcm";
    }

    @Override
    public int getPriority() {
        return config.getPriority();
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    private Message buildMessage(NotificationRequest request, Recipient recipient) {
        String title = String.valueOf(request.parameters().getOrDefault("title", "Notification"));
        String body = String.valueOf(request.parameters().getOrDefault("body", "You have a new notification"));

        Map<String, String> data = new HashMap<>();
        request.parameters().forEach((key, value) -> {
            if (!"title".equals(key) && !"body".equals(key)) {
                data.put(key, String.valueOf(value));
            }
        });

        return Message.builder()
                .setToken(recipient.deviceToken())
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(data)
                .build();
    }

    private boolean isRetryableError(FirebaseMessagingException e) {
        MessagingErrorCode errorCode = e.getMessagingErrorCode();

        return errorCode == MessagingErrorCode.INTERNAL ||
                errorCode == MessagingErrorCode.UNAVAILABLE ||
                errorCode == MessagingErrorCode.QUOTA_EXCEEDED;
    }
}