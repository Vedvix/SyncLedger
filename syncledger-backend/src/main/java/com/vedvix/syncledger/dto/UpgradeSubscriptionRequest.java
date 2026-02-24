package com.vedvix.syncledger.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Request DTO for upgrading subscription plan.
 * 
 * @author vedvix
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpgradeSubscriptionRequest {

    @NotBlank(message = "Plan is required")
    private String plan;  // STARTER, PROFESSIONAL, ENTERPRISE

    private String billingCycle;  // MONTHLY, QUARTERLY, ANNUAL

    // For Stripe payment
    private String paymentMethodId;
}
