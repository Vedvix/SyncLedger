package com.vedvix.syncledger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Request body for creating / updating a coupon.
 *
 * @author vedvix
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponRequest {

    @NotBlank(message = "Coupon code is required")
    private String code;

    private String description;

    @NotBlank(message = "Discount type is required (PERCENTAGE or FIXED_AMOUNT)")
    private String discountType;

    @NotNull(message = "Discount value is required")
    @Min(value = 0, message = "Discount value must be >= 0")
    private Long discountValue;

    /** Comma-separated plan keys. Null or blank = all plans. */
    private String applicablePlans;

    /** Null = unlimited. */
    private Integer maxRedemptions;

    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Boolean active;
}
