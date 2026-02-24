package com.vedvix.syncledger.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO representing a subscription plan definition.
 *
 * @author vedvix
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanDefinitionDTO {
    private Long id;
    private String planKey;
    private String displayName;
    private String description;
    private Long monthlyPrice;
    private Long annualPrice;
    private String invoicesPerMonth;
    private String maxUsers;
    private String maxOrganizations;
    private String maxEmailInboxes;
    private String storage;
    private String approvalType;
    private String supportLevel;
    private String uptimeSla;
    private Boolean highlight;
    private Integer sortOrder;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
