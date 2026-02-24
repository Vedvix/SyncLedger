package com.vedvix.syncledger.notification.dtos;

import com.vedvix.syncledger.notification.enums.NotificationChannel;
import com.vedvix.syncledger.notification.enums.NotificationPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class NotificationRequestDto {

    @NotNull(message = "Channel is required")
    private NotificationChannel channel;

    @NotBlank(message = "Template name is required")
    private String templateName;

    @NotEmpty(message = "At least one recipient is required")
    @Valid
    private List<RecipientDto> recipients;

    private Map<String, Object> parameters;

    private NotificationPriority priority;

    private Instant scheduledAt;

    private Map<String, String> metadata;
}