package com.vedvix.syncledger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.*;

/**
 * Request body for creating / updating a plan definition.
 *
 * @author vedvix
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanDefinitionRequest {

    @NotBlank(message = "Plan key is required")
    private String planKey;

    @NotBlank(message = "Display name is required")
    private String displayName;

    private String description;

    @NotNull(message = "Monthly price is required")
    @Min(value = 0, message = "Monthly price must be >= 0")
    private Long monthlyPrice;

    @NotNull(message = "Annual price is required")
    @Min(value = 0, message = "Annual price must be >= 0")
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
}
