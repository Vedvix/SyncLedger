package com.vedvix.syncledger.controller;

import com.vedvix.syncledger.dto.*;
import com.vedvix.syncledger.security.UserPrincipal;
import com.vedvix.syncledger.service.OrganizationService;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Super Admin Controller for platform-wide management.
 * Only accessible by SUPER_ADMIN role.
 * 
 * @author vedvix
 */
@RestController
@RequestMapping("/api/v1/super-admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Super Admin", description = "API endpoints for platform-wide administration including organization and statistics management")
public class SuperAdminController {

    private final OrganizationService organizationService;

    // ==================== Organization Management ====================

    @PostMapping("/organizations")
    @Operation(
        summary = "Create new organization",
        description = "Creates a new organization on the platform. Only SUPER_ADMIN users can create organizations."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Organization created successfully",
            content = @Content(schema = @Schema(implementation = OrganizationDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid organization data - slug may already exist"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - user not authenticated"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - requires SUPER_ADMIN role"
        )
    })
    public ResponseEntity<ApiResponseDto<OrganizationDTO>> createOrganization(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Organization creation details including name, slug, and email configuration",
                required = true,
                content = @Content(schema = @Schema(implementation = CreateOrganizationRequest.class))
            )
            @Valid @RequestBody CreateOrganizationRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {
        OrganizationDTO org = organizationService.createOrganization(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Organization created successfully", org));
    }

    @GetMapping("/organizations")
    @Operation(
        summary = "Get all organizations",
        description = "Retrieves a paginated list of all organizations on the platform"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponse(
        responseCode = "200",
        description = "Organizations retrieved successfully"
    )
    public ResponseEntity<ApiResponseDto<PagedResponse<OrganizationDTO>>> getAllOrganizations(
            @Parameter(description = "Pagination information (page, size, sort)", required = false)
            @PageableDefault(size = 20) Pageable pageable,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {
        PagedResponse<OrganizationDTO> orgs = organizationService.getAllOrganizations(pageable, currentUser);
        return ResponseEntity.ok(ApiResponseDto.success(orgs));
    }

    @GetMapping("/organizations/{id}")
    @Operation(
        summary = "Get organization by ID",
        description = "Retrieves detailed information about a specific organization"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Organization found and returned successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - user not authenticated"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - requires SUPER_ADMIN role"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Organization not found"
        )
    })
    public ResponseEntity<ApiResponseDto<OrganizationDTO>> getOrganization(
            @Parameter(description = "Organization ID", required = true)
            @PathVariable Long id,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {
        OrganizationDTO org = organizationService.getOrganizationById(id, currentUser);
        return ResponseEntity.ok(ApiResponseDto.success(org));
    }

    @GetMapping("/organizations/slug/{slug}")
    @Operation(
        summary = "Get organization by slug",
        description = "Retrieves an organization by its unique slug identifier"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Organization found and returned successfully"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Organization with given slug not found"
        )
    })
    public ResponseEntity<ApiResponseDto<OrganizationDTO>> getOrganizationBySlug(
            @Parameter(description = "Organization slug", required = true)
            @PathVariable String slug,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {
        OrganizationDTO org = organizationService.getOrganizationBySlug(slug, currentUser);
        return ResponseEntity.ok(ApiResponseDto.success(org));
    }

    @PutMapping("/organizations/{id}")
    @Operation(
        summary = "Update organization",
        description = "Updates organization information such as name, email address, and configuration"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Organization updated successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid update request"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - user not authenticated"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - requires SUPER_ADMIN role"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Organization not found"
        )
    })
    public ResponseEntity<ApiResponseDto<OrganizationDTO>> updateOrganization(
            @Parameter(description = "Organization ID to update", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Organization update details",
                required = true,
                content = @Content(schema = @Schema(implementation = UpdateOrganizationRequest.class))
            )
            @Valid @RequestBody UpdateOrganizationRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {
        OrganizationDTO org = organizationService.updateOrganization(id, request, currentUser);
        return ResponseEntity.ok(ApiResponseDto.success("Organization updated successfully", org));
    }

    @PostMapping("/organizations/{id}/activate")
    @Operation(
        summary = "Activate organization",
        description = "Activates a suspended or inactive organization, allowing it to process invoices again"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Organization activated successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Organization is already active"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Organization not found"
        )
    })
    public ResponseEntity<ApiResponseDto<OrganizationDTO>> activateOrganization(
            @Parameter(description = "Organization ID to activate", required = true)
            @PathVariable Long id,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {
        OrganizationDTO org = organizationService.activateOrganization(id, currentUser);
        return ResponseEntity.ok(ApiResponseDto.success("Organization activated", org));
    }

    @PostMapping("/organizations/{id}/suspend")
    @Operation(
        summary = "Suspend organization",
        description = "Suspends an active organization, preventing it from processing new invoices"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Organization suspended successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Organization is already suspended"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Organization not found"
        )
    })
    public ResponseEntity<ApiResponseDto<OrganizationDTO>> suspendOrganization(
            @Parameter(description = "Organization ID to suspend", required = true)
            @PathVariable Long id,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {
        OrganizationDTO org = organizationService.suspendOrganization(id, currentUser);
        return ResponseEntity.ok(ApiResponseDto.success("Organization suspended", org));
    }

    @GetMapping("/organizations/{id}/stats")
    @Operation(
        summary = "Get organization statistics",
        description = "Retrieves detailed statistics for a specific organization including invoice counts, user metrics, and approval rates"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Organization statistics retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Organization not found"
        )
    })
    public ResponseEntity<ApiResponseDto<OrganizationStatsDTO>> getOrganizationStats(
            @Parameter(description = "Organization ID", required = true)
            @PathVariable Long id,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {
        OrganizationStatsDTO stats = organizationService.getOrganizationStats(id, currentUser);
        return ResponseEntity.ok(ApiResponseDto.success(stats));
    }

    // ==================== Platform Dashboard ====================

    @GetMapping("/dashboard/stats")
    @Operation(
        summary = "Get platform statistics",
        description = "Retrieves platform-wide statistics including total organizations, users, invoices, and system health metrics"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Platform statistics retrieved successfully",
            content = @Content(schema = @Schema(implementation = PlatformStatsDTO.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - user not authenticated"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - requires SUPER_ADMIN role"
        )
    })
    public ResponseEntity<ApiResponseDto<PlatformStatsDTO>> getPlatformStats(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {
        PlatformStatsDTO stats = organizationService.getPlatformStats(currentUser);
        return ResponseEntity.ok(ApiResponseDto.success(stats));
    }
}

