package com.vedvix.syncledger.notification.domain;

import com.vedvix.syncledger.notification.enums.NotificationStatus;

import java.time.Instant;

public record NotificationResult(
        String notificationId,
        String recipientId,
        NotificationStatus status,
        String providerId,
        String externalId,
        String errorMessage,
        Instant sentAt,
        Instant deliveredAt,
        int attemptCount
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String notificationId;
        private String recipientId;
        private NotificationStatus status;
        private String providerId;
        private String externalId;
        private String errorMessage;
        private Instant sentAt;
        private Instant deliveredAt;
        private int attemptCount;

        public Builder notificationId(String notificationId) { this.notificationId = notificationId; return this; }
        public Builder recipientId(String recipientId) { this.recipientId = recipientId; return this; }
        public Builder status(NotificationStatus status) { this.status = status; return this; }
        public Builder providerId(String providerId) { this.providerId = providerId; return this; }
        public Builder externalId(String externalId) { this.externalId = externalId; return this; }
        public Builder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        public Builder sentAt(Instant sentAt) { this.sentAt = sentAt; return this; }
        public Builder deliveredAt(Instant deliveredAt) { this.deliveredAt = deliveredAt; return this; }
        public Builder attemptCount(int attemptCount) { this.attemptCount = attemptCount; return this; }

        public NotificationResult build() {
            return new NotificationResult(notificationId, recipientId, status, providerId,
                    externalId, errorMessage, sentAt, deliveredAt, attemptCount);
        }
    }
}