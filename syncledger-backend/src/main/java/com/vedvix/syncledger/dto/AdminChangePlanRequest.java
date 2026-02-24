package com.vedvix.syncledger.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Request DTO for Super Admin to directly change an organization's subscription plan.
 * This bypasses Stripe checkout – useful for granting access, comp plans, etc.
 *
 * @author vedvix
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminChangePlanRequest {

    @NotBlank(message = "Plan is required")
    private String plan;  // STARTER, PROFESSIONAL, BUSINESS, ENTERPRISE

    private String billingCycle;  // MONTHLY or ANNUAL (default MONTHLY)

    /** Subscription duration in days (default 365) */
    private Integer durationDays;
}
