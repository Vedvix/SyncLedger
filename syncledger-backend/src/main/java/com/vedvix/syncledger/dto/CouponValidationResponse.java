package com.vedvix.syncledger.dto;

import lombok.*;

/**
 * Response returned when a coupon code is validated at checkout.
 *
 * @author vedvix
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponValidationResponse {
    private Boolean valid;
    private String code;
    private String discountType;
    private Long discountValue;
    private Long originalPrice;
    private Long discountedPrice;
    private Long discountAmount;
    private String message;
}
