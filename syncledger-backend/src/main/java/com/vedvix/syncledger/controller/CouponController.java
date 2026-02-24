package com.vedvix.syncledger.controller;

import com.vedvix.syncledger.dto.*;
import com.vedvix.syncledger.security.UserPrincipal;
import com.vedvix.syncledger.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for coupon / voucher management.
 * Public endpoint validates a code; admin endpoints CRUD coupons.
 *
 * @author vedvix
 */
@RestController
@RequestMapping("/v1/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupons", description = "Voucher / coupon code management")
public class CouponController {

    private final CouponService service;

    // ==================== Public: validate coupon ====================

    @PostMapping("/validate")
    @Operation(summary = "Validate coupon code", description = "Check if a coupon code is valid for a given plan")
    public ResponseEntity<ApiResponseDto<CouponValidationResponse>> validateCoupon(
            @Valid @RequestBody ApplyCouponRequest request,
            @RequestParam(defaultValue = "MONTHLY") String billingCycle) {
        CouponValidationResponse result = service.validateCoupon(
                request.getCode(), request.getPlanKey(), billingCycle);
        return ResponseEntity.ok(ApiResponseDto.success(result));
    }

    // ==================== Super Admin ====================

    @GetMapping
    @Operation(summary = "Get all coupons (Super Admin)")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponseDto<List<CouponDTO>>> getAllCoupons() {
        return ResponseEntity.ok(ApiResponseDto.success(service.getAllCoupons()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get coupon by ID (Super Admin)")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponseDto<CouponDTO>> getCouponById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDto.success(service.getCouponById(id)));
    }

    @PostMapping
    @Operation(summary = "Create coupon (Super Admin)")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponseDto<CouponDTO>> createCoupon(
            @Valid @RequestBody CouponRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ApiResponseDto.success("Coupon created", service.createCoupon(request, user.getId())));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update coupon (Super Admin)")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponseDto<CouponDTO>> updateCoupon(
            @PathVariable Long id,
            @Valid @RequestBody CouponRequest request) {
        return ResponseEntity.ok(ApiResponseDto.success("Coupon updated", service.updateCoupon(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate coupon (Super Admin)")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponseDto<Void>> deactivateCoupon(@PathVariable Long id) {
        service.deactivateCoupon(id);
        return ResponseEntity.ok(ApiResponseDto.success("Coupon deactivated"));
    }
}
