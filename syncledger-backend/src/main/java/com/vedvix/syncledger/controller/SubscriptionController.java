package com.vedvix.syncledger.controller;

import com.vedvix.syncledger.dto.*;
import com.vedvix.syncledger.security.UserPrincipal;
import com.vedvix.syncledger.service.PlanDefinitionService;
import com.vedvix.syncledger.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Subscription management controller.
 * Allows org admins to view/upgrade subscriptions.
 * Super admins can manage any organization's subscription.
 * 
 * @author vedvix
 */
@RestController
@RequestMapping("/v1/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscriptions", description = "API for subscription and billing management")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final PlanDefinitionService planDefinitionService;

    @GetMapping("/current")
    @Operation(
        summary = "Get current subscription",
        description = "Returns the subscription details for the current user's organization"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponse(responseCode = "200", description = "Subscription details returned")
    public ResponseEntity<ApiResponseDto<SubscriptionDTO>> getCurrentSubscription(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Long orgId = currentUser.getOrganizationId();
        if (orgId == null) {
            return ResponseEntity.ok(ApiResponseDto.error("Super admin accounts do not have subscriptions"));
        }

        SubscriptionDTO subscription = subscriptionService.getSubscription(orgId);
        return ResponseEntity.ok(ApiResponseDto.success(subscription));
    }

    @PostMapping("/upgrade")
    @Operation(
        summary = "Upgrade subscription plan",
        description = "Creates a Stripe Checkout Session for upgrading. Returns a checkoutUrl for the frontend to redirect to."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Checkout session created — redirect to checkoutUrl"),
        @ApiResponse(responseCode = "400", description = "Invalid plan or request"),
        @ApiResponse(responseCode = "403", description = "Only admins can upgrade subscriptions")
    })
    public ResponseEntity<ApiResponseDto<SubscriptionDTO>> upgradeSubscription(
            @Valid @RequestBody UpgradeSubscriptionRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Long orgId = currentUser.getOrganizationId();
        if (orgId == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("Super admin accounts do not have subscriptions"));
        }

        SubscriptionDTO subscription = subscriptionService.upgradeSubscription(
                orgId, request, currentUser.getId());

        String message = subscription.getCheckoutUrl() != null
                ? "Checkout session created. Redirect to checkoutUrl to complete payment."
                : "Subscription upgraded successfully";
        return ResponseEntity.ok(ApiResponseDto.success(message, subscription));
    }

    @PostMapping("/cancel")
    @Operation(
        summary = "Cancel subscription",
        description = "Cancel the current subscription. Access continues until the end of the billing period."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Subscription cancelled"),
        @ApiResponse(responseCode = "400", description = "Subscription already cancelled")
    })
    public ResponseEntity<ApiResponseDto<SubscriptionDTO>> cancelSubscription(
            @RequestParam(required = false) String reason,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Long orgId = currentUser.getOrganizationId();
        if (orgId == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("Super admin accounts do not have subscriptions"));
        }

        SubscriptionDTO subscription = subscriptionService.cancelSubscription(
                orgId, reason, currentUser.getId());
        return ResponseEntity.ok(ApiResponseDto.success("Subscription cancelled", subscription));
    }

    @GetMapping("/plans")
    @Operation(
        summary = "Get available plans",
        description = "Returns all active subscription plans with pricing from the database"
    )
    public ResponseEntity<ApiResponseDto<Object>> getAvailablePlans() {
        // Delegate to the database-backed PlanDefinitionService
        var plans = planDefinitionService.getActivePlans();
        return ResponseEntity.ok(ApiResponseDto.success(plans));
    }

    // ==================== Super Admin Subscription Management ====================

    @GetMapping("/admin/{organizationId}")
    @Operation(
        summary = "Get subscription for organization (Super Admin)",
        description = "Super Admin can view any organization's subscription"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponseDto<SubscriptionDTO>> getSubscriptionByOrg(
            @PathVariable Long organizationId) {

        SubscriptionDTO subscription = subscriptionService.getSubscription(organizationId);
        return ResponseEntity.ok(ApiResponseDto.success(subscription));
    }

    @PostMapping("/admin/{organizationId}/reactivate")
    @Operation(
        summary = "Reactivate subscription (Super Admin)",
        description = "Super Admin can reactivate a suspended/expired subscription with additional days"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Subscription reactivated"),
        @ApiResponse(responseCode = "404", description = "Subscription not found")
    })
    public ResponseEntity<ApiResponseDto<SubscriptionDTO>> reactivateSubscription(
            @PathVariable Long organizationId,
            @RequestParam(defaultValue = "15") int additionalDays,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {

        SubscriptionDTO subscription = subscriptionService.reactivateSubscription(
                organizationId, additionalDays, currentUser.getId());
        return ResponseEntity.ok(ApiResponseDto.success("Subscription reactivated", subscription));
    }

    @PutMapping("/admin/{organizationId}/plan")
    @Operation(
        summary = "Change organization plan (Super Admin)",
        description = "Super Admin can directly change an organization's subscription plan, bypassing Stripe checkout"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Subscription plan changed"),
        @ApiResponse(responseCode = "400", description = "Invalid plan"),
        @ApiResponse(responseCode = "404", description = "Subscription not found")
    })
    public ResponseEntity<ApiResponseDto<SubscriptionDTO>> adminChangePlan(
            @PathVariable Long organizationId,
            @Valid @RequestBody AdminChangePlanRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {

        SubscriptionDTO subscription = subscriptionService.adminChangePlan(
                organizationId, request, currentUser.getId());
        return ResponseEntity.ok(ApiResponseDto.success("Subscription plan updated", subscription));
    }
}
