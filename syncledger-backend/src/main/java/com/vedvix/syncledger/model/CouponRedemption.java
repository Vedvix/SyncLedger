package com.vedvix.syncledger.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Audit trail — which organization redeemed which coupon.
 *
 * @author vedvix
 */
@Entity
@Table(name = "coupon_redemptions", indexes = {
        @Index(name = "idx_coupon_redemptions_coupon", columnList = "coupon_id"),
        @Index(name = "idx_coupon_redemptions_org",    columnList = "organization_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponRedemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "plan_key", length = 50)
    private String planKey;

    /** Actual discount applied in cents. */
    @Column(name = "discount_amount", nullable = false)
    @Builder.Default
    private Long discountAmount = 0L;

    @Column(name = "redeemed_by")
    private Long redeemedBy;

    @Column(name = "redeemed_at", nullable = false)
    @Builder.Default
    private LocalDateTime redeemedAt = LocalDateTime.now();
}
