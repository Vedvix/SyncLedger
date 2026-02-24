package com.vedvix.syncledger.service;

import com.vedvix.syncledger.dto.*;
import com.vedvix.syncledger.exception.ResourceNotFoundException;
import com.vedvix.syncledger.model.*;
import com.vedvix.syncledger.repository.CouponRedemptionRepository;
import com.vedvix.syncledger.repository.CouponRepository;
import com.vedvix.syncledger.repository.PlanDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing coupon / voucher codes and redemptions.
 *
 * @author vedvix
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepo;
    private final CouponRedemptionRepository redemptionRepo;
    private final PlanDefinitionRepository planRepo;

    // ==================== Admin CRUD ====================

    @Transactional(readOnly = true)
    public List<CouponDTO> getAllCoupons() {
        return couponRepo.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public CouponDTO getCouponById(Long id) {
        Coupon coupon = couponRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", id));
        return toDTO(coupon);
    }

    @Transactional
    public CouponDTO createCoupon(CouponRequest request, Long createdBy) {
        if (couponRepo.existsByCodeIgnoreCase(request.getCode().trim())) {
            throw new IllegalArgumentException("Coupon code '" + request.getCode() + "' already exists");
        }

        DiscountType type;
        try {
            type = DiscountType.valueOf(request.getDiscountType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid discount type: " + request.getDiscountType());
        }

        Coupon coupon = Coupon.builder()
                .code(request.getCode().trim().toUpperCase())
                .description(request.getDescription())
                .discountType(type)
                .discountValue(request.getDiscountValue())
                .applicablePlans(request.getApplicablePlans())
                .maxRedemptions(request.getMaxRedemptions())
                .validFrom(request.getValidFrom() != null ? request.getValidFrom() : LocalDateTime.now())
                .validUntil(request.getValidUntil())
                .active(request.getActive() != null ? request.getActive() : true)
                .createdBy(createdBy)
                .build();

        coupon = couponRepo.save(coupon);
        log.info("Created coupon: {} (type={}, value={})", coupon.getCode(), type, coupon.getDiscountValue());
        return toDTO(coupon);
    }

    @Transactional
    public CouponDTO updateCoupon(Long id, CouponRequest request) {
        Coupon coupon = couponRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", id));

        String newCode = request.getCode().trim().toUpperCase();
        if (!coupon.getCode().equals(newCode) && couponRepo.existsByCodeIgnoreCase(newCode)) {
            throw new IllegalArgumentException("Coupon code '" + newCode + "' already exists");
        }

        DiscountType type;
        try {
            type = DiscountType.valueOf(request.getDiscountType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid discount type: " + request.getDiscountType());
        }

        coupon.setCode(newCode);
        coupon.setDescription(request.getDescription());
        coupon.setDiscountType(type);
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setApplicablePlans(request.getApplicablePlans());
        coupon.setMaxRedemptions(request.getMaxRedemptions());
        if (request.getValidFrom() != null) coupon.setValidFrom(request.getValidFrom());
        coupon.setValidUntil(request.getValidUntil());
        if (request.getActive() != null) coupon.setActive(request.getActive());

        coupon = couponRepo.save(coupon);
        log.info("Updated coupon: {}", coupon.getCode());
        return toDTO(coupon);
    }

    @Transactional
    public void deactivateCoupon(Long id) {
        Coupon coupon = couponRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", id));
        coupon.setActive(false);
        couponRepo.save(coupon);
        log.info("Deactivated coupon: {}", coupon.getCode());
    }

    // ==================== Public: validate / apply ====================

    /**
     * Validate a coupon code for a given plan.
     * Returns discount details without recording a redemption.
     */
    @Transactional(readOnly = true)
    public CouponValidationResponse validateCoupon(String code, String planKey, String billingCycle) {
        var optCoupon = couponRepo.findByCodeIgnoreCase(code.trim());
        if (optCoupon.isEmpty()) {
            return CouponValidationResponse.builder()
                    .valid(false)
                    .code(code)
                    .message("Invalid coupon code")
                    .build();
        }

        Coupon coupon = optCoupon.get();

        if (!coupon.isValid()) {
            String reason = !coupon.getActive() ? "Coupon is no longer active"
                    : coupon.getMaxRedemptions() != null && coupon.getCurrentRedemptions() >= coupon.getMaxRedemptions()
                        ? "Coupon has reached its maximum number of redemptions"
                        : "Coupon has expired";
            return CouponValidationResponse.builder()
                    .valid(false)
                    .code(code)
                    .message(reason)
                    .build();
        }

        if (!coupon.appliesTo(planKey)) {
            return CouponValidationResponse.builder()
                    .valid(false)
                    .code(code)
                    .message("This coupon is not applicable to the selected plan")
                    .build();
        }

        // Look up plan price
        var optPlan = planRepo.findByPlanKey(planKey);
        if (optPlan.isEmpty()) {
            return CouponValidationResponse.builder()
                    .valid(false)
                    .code(code)
                    .message("Invalid plan")
                    .build();
        }

        PlanDefinition plan = optPlan.get();
        long originalPrice = "ANNUAL".equalsIgnoreCase(billingCycle)
                ? plan.getAnnualPrice()
                : plan.getMonthlyPrice();

        long discountAmount = calculateDiscount(coupon, originalPrice);
        long discountedPrice = Math.max(0, originalPrice - discountAmount);

        return CouponValidationResponse.builder()
                .valid(true)
                .code(coupon.getCode())
                .discountType(coupon.getDiscountType().name())
                .discountValue(coupon.getDiscountValue())
                .originalPrice(originalPrice)
                .discountedPrice(discountedPrice)
                .discountAmount(discountAmount)
                .message("Coupon applied successfully!")
                .build();
    }

    /**
     * Record a coupon redemption (called during checkout completion).
     */
    @Transactional
    public long redeemCoupon(String code, String planKey, Long organizationId, Long userId, String billingCycle) {
        Coupon coupon = couponRepo.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid coupon code"));

        if (!coupon.isValid()) {
            throw new IllegalArgumentException("Coupon is no longer valid");
        }
        if (!coupon.appliesTo(planKey)) {
            throw new IllegalArgumentException("Coupon does not apply to this plan");
        }

        PlanDefinition plan = planRepo.findByPlanKey(planKey)
                .orElseThrow(() -> new IllegalArgumentException("Invalid plan"));

        long originalPrice = "ANNUAL".equalsIgnoreCase(billingCycle)
                ? plan.getAnnualPrice()
                : plan.getMonthlyPrice();
        long discountAmount = calculateDiscount(coupon, originalPrice);

        // Record redemption
        CouponRedemption redemption = CouponRedemption.builder()
                .coupon(coupon)
                .organization(Organization.builder().id(organizationId).build())
                .planKey(planKey)
                .discountAmount(discountAmount)
                .redeemedBy(userId)
                .build();
        redemptionRepo.save(redemption);

        // Increment counter
        coupon.setCurrentRedemptions(coupon.getCurrentRedemptions() + 1);
        couponRepo.save(coupon);

        log.info("Coupon {} redeemed by org {} for plan {} — discount {} cents",
                coupon.getCode(), organizationId, planKey, discountAmount);

        return discountAmount;
    }

    // ==================== Helpers ====================

    private long calculateDiscount(Coupon coupon, long originalPrice) {
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            return (originalPrice * coupon.getDiscountValue()) / 100;
        } else {
            return Math.min(coupon.getDiscountValue(), originalPrice);
        }
    }

    private CouponDTO toDTO(Coupon c) {
        return CouponDTO.builder()
                .id(c.getId())
                .code(c.getCode())
                .description(c.getDescription())
                .discountType(c.getDiscountType().name())
                .discountValue(c.getDiscountValue())
                .applicablePlans(c.getApplicablePlans())
                .maxRedemptions(c.getMaxRedemptions())
                .currentRedemptions(c.getCurrentRedemptions())
                .validFrom(c.getValidFrom())
                .validUntil(c.getValidUntil())
                .active(c.getActive())
                .valid(c.isValid())
                .createdBy(c.getCreatedBy())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
