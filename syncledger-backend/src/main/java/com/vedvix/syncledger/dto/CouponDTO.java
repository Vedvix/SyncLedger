package com.vedvix.syncledger.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO representing a coupon / voucher code.
 *
 * @author vedvix
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponDTO {
    private Long id;
    private String code;
    private String description;
    private String discountType;          // PERCENTAGE | FIXED_AMOUNT
    private Long discountValue;
    private String applicablePlans;       // comma-separated plan keys or null
    private Integer maxRedemptions;
    private Integer currentRedemptions;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Boolean active;
    private Boolean valid;                // computed: currently redeemable?
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
