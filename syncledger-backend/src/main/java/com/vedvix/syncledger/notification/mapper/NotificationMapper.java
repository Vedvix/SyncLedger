package com.vedvix.syncledger.notification.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vedvix.syncledger.notification.domain.NotificationRequest;
import com.vedvix.syncledger.notification.domain.Recipient;
import com.vedvix.syncledger.notification.dtos.NotificationRequestDto;
import com.vedvix.syncledger.notification.dtos.NotificationResponseDto;
import com.vedvix.syncledger.notification.dtos.RecipientDto;
import com.vedvix.syncledger.notification.entity.NotificationEntity;
import com.vedvix.syncledger.notification.enums.NotificationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationMapper {

    private final ObjectMapper objectMapper;

    // DTO to Domain
    public NotificationRequest toNotificationRequest(NotificationRequestDto dto) {
        return NotificationRequest.builder()
                .channel(dto.getChannel())
                .templateName(dto.getTemplateName())
                .recipients(dto.getRecipients().stream()
                        .map(this::toRecipient)
                        .toList())
                .parameters(dto.getParameters())
                .priority(dto.getPriority())
                .scheduledAt(dto.getScheduledAt())
                .metadata(dto.getMetadata())
                .build();
    }

    public Recipient toRecipient(RecipientDto dto) {
        return new Recipient(
                dto.getEmail(),
                dto.getPhoneNumber(),
                dto.getDeviceToken(),
                dto.getAttributes()
        );
    }

    // Domain to Entity
    public NotificationEntity toEntity(NotificationRequest request) {
        return NotificationEntity.builder()
                .notificationId(request.id())
                .channel(request.channel())
                .templateName(request.templateName())
                .recipients(request.recipients())
                .parametersJson(serializeToJson(request.parameters()))
                .priority(request.priority())
                .scheduledAt(request.scheduledAt())
                .metadataJson(serializeToJson(request.metadata()))
                .tenantId(request.tenantId())
                .status(NotificationStatus.PENDING)
                .createdAt(request.createdAt())
                .attemptCount(0)
                .build();
    }

    // Entity to Domain
    public NotificationRequest toDomain(NotificationEntity entity) {
        return NotificationRequest.builder()
                .id(entity.getNotificationId())
                .channel(entity.getChannel())
                .templateName(entity.getTemplateName())
                .recipients(entity.getRecipients())
                .parameters(deserializeMap(entity.getParametersJson()))
                .priority(entity.getPriority())
                .scheduledAt(entity.getScheduledAt())
                .metadata(deserializeStringMap(entity.getMetadataJson()))
                .tenantId(entity.getTenantId())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    // Entity to Response DTO
    public NotificationResponseDto toResponseDto(NotificationEntity entity) {
        return NotificationResponseDto.builder()
                .id(entity.getNotificationId())
                .channel(entity.getChannel())
                .templateName(entity.getTemplateName())
                .recipients(entity.getRecipients().stream()
                        .map(this::toRecipientDto)
                        .toList())
                .status(entity.getStatus())
                .priority(entity.getPriority())
                .scheduledAt(entity.getScheduledAt())
                .createdAt(entity.getCreatedAt())
                .sentAt(entity.getSentAt())
                .metadata(deserializeStringMap(entity.getMetadataJson()))
                .errorMessage(entity.getErrorMessage())
                .attemptCount(entity.getAttemptCount())
                .build();
    }

    public NotificationResponseDto toResponseDto(NotificationRequest request) {
        return NotificationResponseDto.builder()
                .id(request.id())
                .channel(request.channel())
                .templateName(request.templateName())
                .recipients(request.recipients().stream()
                        .map(this::toRecipientDto)
                        .toList())
                .status(NotificationStatus.PENDING)
                .priority(request.priority())
                .scheduledAt(request.scheduledAt())
                .createdAt(request.createdAt())
                .metadata(request.metadata())
                .build();
    }

    public RecipientDto toRecipientDto(Recipient recipient) {
        return RecipientDto.builder()
                .email(recipient.email())
                .phoneNumber(recipient.phoneNumber())
                .deviceToken(recipient.deviceToken())
                .attributes(recipient.attributes())
                .build();
    }

    // JSON Serialization helpers
    private String serializeToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }

    private List<Recipient> deserializeRecipients(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Recipient>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize recipients", e);
        }
    }

    private Map<String, Object> deserializeMap(String json) {
        try {
            if (json == null || json.isBlank()) {
                return Map.of();
            }
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize map", e);
        }
    }

    private Map<String, String> deserializeStringMap(String json) {
        try {
            if (json == null || json.isBlank()) {
                return Map.of();
            }
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize string map", e);
        }
    }
}