package com.vedvix.syncledger.controller;

import com.vedvix.syncledger.dto.*;
import com.vedvix.syncledger.security.UserPrincipal;
import com.vedvix.syncledger.service.PlanDefinitionService;
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
 * REST controller for subscription plan definitions.
 * Public endpoint returns active plans; admin endpoints allow full CRUD.
 *
 * @author vedvix
 */
@RestController
@RequestMapping("/v1/plan-definitions")
@RequiredArgsConstructor
@Tag(name = "Plan Definitions", description = "Subscription plan catalog management")
public class PlanDefinitionController {

    private final PlanDefinitionService service;

    // ==================== Public ====================

    @GetMapping("/active")
    @Operation(summary = "Get active plans", description = "Returns all active plans for the pricing page (public)")
    public ResponseEntity<ApiResponseDto<List<PlanDefinitionDTO>>> getActivePlans() {
        return ResponseEntity.ok(ApiResponseDto.success(service.getActivePlans()));
    }

    // ==================== Super Admin ====================

    @GetMapping
    @Operation(summary = "Get all plans (Super Admin)")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponseDto<List<PlanDefinitionDTO>>> getAllPlans() {
        return ResponseEntity.ok(ApiResponseDto.success(service.getAllPlans()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get plan by ID (Super Admin)")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponseDto<PlanDefinitionDTO>> getPlanById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDto.success(service.getPlanById(id)));
    }

    @PostMapping
    @Operation(summary = "Create plan (Super Admin)")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponseDto<PlanDefinitionDTO>> createPlan(
            @Valid @RequestBody PlanDefinitionRequest request) {
        return ResponseEntity.ok(ApiResponseDto.success("Plan created", service.createPlan(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update plan (Super Admin)")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponseDto<PlanDefinitionDTO>> updatePlan(
            @PathVariable Long id,
            @Valid @RequestBody PlanDefinitionRequest request) {
        return ResponseEntity.ok(ApiResponseDto.success("Plan updated", service.updatePlan(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate plan (Super Admin)")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponseDto<Void>> deletePlan(@PathVariable Long id) {
        service.deletePlan(id);
        return ResponseEntity.ok(ApiResponseDto.success("Plan deactivated"));
    }
}
