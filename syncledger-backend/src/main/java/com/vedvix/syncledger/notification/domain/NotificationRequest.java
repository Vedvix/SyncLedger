package com.vedvix.syncledger.notification.domain;

import com.vedvix.syncledger.notification.enums.NotificationChannel;
import com.vedvix.syncledger.notification.enums.NotificationPriority;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record NotificationRequest(
        String id,
        NotificationChannel channel,
        String templateName,
        List<Recipient> recipients,
        Map<String, Object> parameters,
        NotificationPriority priority,
        Instant scheduledAt,
        Map<String, String> metadata,
        String tenantId,
        Instant createdAt
) {
    public NotificationRequest {
        id = id == null ? UUID.randomUUID().toString() : id;
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        priority = priority == null ? NotificationPriority.MEDIUM : priority;
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private NotificationChannel channel;
        private String templateName;
        private List<Recipient> recipients;
        private Map<String, Object> parameters;
        private NotificationPriority priority;
        private Instant scheduledAt;
        private Map<String, String> metadata;
        private String tenantId;
        private Instant createdAt;

        public Builder id(String id) { this.id = id; return this; }
        public Builder channel(NotificationChannel channel) { this.channel = channel; return this; }
        public Builder templateName(String templateName) { this.templateName = templateName; return this; }
        public Builder recipients(List<Recipient> recipients) { this.recipients = recipients; return this; }
        public Builder parameters(Map<String, Object> parameters) { this.parameters = parameters; return this; }
        public Builder priority(NotificationPriority priority) { this.priority = priority; return this; }
        public Builder scheduledAt(Instant scheduledAt) { this.scheduledAt = scheduledAt; return this; }
        public Builder metadata(Map<String, String> metadata) { this.metadata = metadata; return this; }
        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public NotificationRequest build() {
            return new NotificationRequest(id, channel, templateName, recipients, parameters,
                    priority, scheduledAt, metadata, tenantId, createdAt);
        }
    }
}