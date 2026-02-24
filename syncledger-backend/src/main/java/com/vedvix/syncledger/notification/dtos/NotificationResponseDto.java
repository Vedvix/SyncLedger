package com.vedvix.syncledger.notification.dtos;

import com.vedvix.syncledger.notification.enums.NotificationChannel;
import com.vedvix.syncledger.notification.enums.NotificationPriority;
import com.vedvix.syncledger.notification.enums.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDto {

    private String id;
    private NotificationChannel channel;
    private String templateName;
    private List<RecipientDto> recipients;
    private NotificationStatus status;
    private NotificationPriority priority;
    private Instant scheduledAt;
    private Instant createdAt;
    private Instant sentAt;
    private Map<String, String> metadata;
    private String errorMessage;
    private int attemptCount;
}