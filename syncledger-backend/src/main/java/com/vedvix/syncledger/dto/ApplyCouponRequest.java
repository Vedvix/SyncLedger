package com.vedvix.syncledger.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Request body for validating / applying a coupon code at checkout.
 *
 * @author vedvix
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplyCouponRequest {

    @NotBlank(message = "Coupon code is required")
    private String code;

    /** Plan the user is subscribing to. */
    @NotBlank(message = "Plan key is required")
    private String planKey;
}
