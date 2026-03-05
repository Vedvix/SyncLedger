package com.vedvix.syncledger.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for invoice audit trail events — returned to the frontend timeline.
 *
 * @author vedvix
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceAuditEventDTO {

    private Long id;
    private Long invoiceId;
    private String eventType;
    private String eventDisplayName;
    private String fromStatus;
    private String toStatus;
    private Long performedByUserId;
    private String performedByName;
    private String description;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
}
