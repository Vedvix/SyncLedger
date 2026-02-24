package com.vedvix.syncledger.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Voucher / coupon codes managed by Super Admin.
 * Supports percentage or fixed-amount discounts, optional plan restriction,
 * max-redemption cap, and validity window.
 *
 * @author vedvix
 */
@Entity
@Table(name = "coupons", indexes = {
        @Index(name = "idx_coupons_code",   columnList = "code"),
        @Index(name = "idx_coupons_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    @Builder.Default
    private DiscountType discountType = DiscountType.PERCENTAGE;

    /** Percentage (e.g. 20 for 20 %) or fixed amount in cents. */
    @Column(name = "discount_value", nullable = false)
    @Builder.Default
    private Long discountValue = 0L;

    /** Comma-separated plan keys this coupon applies to. NULL = all plans. */
    @Column(name = "applicable_plans", length = 255)
    private String applicablePlans;

    /** Maximum number of times this coupon can be redeemed. NULL = unlimited. */
    @Column(name = "max_redemptions")
    private Integer maxRedemptions;

    @Column(name = "current_redemptions", nullable = false)
    @Builder.Default
    private Integer currentRedemptions = 0;

    @Column(name = "valid_from", nullable = false)
    @Builder.Default
    private LocalDateTime validFrom = LocalDateTime.now();

    /** NULL = no expiry. */
    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column
    private LocalDateTime updatedAt;

    // ---------- helpers ----------

    public boolean isValid() {
        if (!active) return false;
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(validFrom)) return false;
        if (validUntil != null && now.isAfter(validUntil)) return false;
        if (maxRedemptions != null && currentRedemptions >= maxRedemptions) return false;
        return true;
    }

    public boolean appliesTo(String planKey) {
        if (applicablePlans == null || applicablePlans.isBlank()) return true;
        for (String p : applicablePlans.split(",")) {
            if (p.trim().equalsIgnoreCase(planKey)) return true;
        }
        return false;
    }
}
