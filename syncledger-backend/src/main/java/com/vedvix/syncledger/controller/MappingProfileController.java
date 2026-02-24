package com.vedvix.syncledger.controller;

import com.vedvix.syncledger.dto.ApiResponseDto;
import com.vedvix.syncledger.dto.MappingProfileDTO;
import com.vedvix.syncledger.dto.MappingProfileRequest;
import com.vedvix.syncledger.exception.BadRequestException;
import com.vedvix.syncledger.security.UserPrincipal;
import com.vedvix.syncledger.service.MappingProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing organization-scoped mapping profiles.
 * Profiles define how invoice fields are mapped/extracted for each org's ERP system.
 *
 * @author vedvix
 */
@Slf4j
@RestController
@RequestMapping("/v1/mapping-profiles")
@RequiredArgsConstructor
@Tag(name = "Mapping Profiles", description = "API for managing organization-scoped invoice field mapping profiles")
public class MappingProfileController {

    private final MappingProfileService mappingProfileService;

    @GetMapping
    @Operation(summary = "List mapping profiles", description = "Get all mapping profiles for the current user's organization")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'USER')")
    public ResponseEntity<ApiResponseDto<List<MappingProfileDTO>>> listProfiles(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(required = false) Long organizationId) {

        Long orgId = resolveOrgId(currentUser, organizationId);
        List<MappingProfileDTO> profiles = mappingProfileService.getProfilesByOrganization(orgId);
        return ResponseEntity.ok(ApiResponseDto.success(profiles));
    }

    @GetMapping("/{profileId}")
    @Operation(summary = "Get mapping profile", description = "Get a single mapping profile by ID")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'USER')")
    public ResponseEntity<ApiResponseDto<MappingProfileDTO>> getProfile(
            @PathVariable String profileId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(required = false) Long organizationId) {

        Long orgId = resolveOrgId(currentUser, organizationId);
        MappingProfileDTO profile = mappingProfileService.getProfile(profileId, orgId);
        return ResponseEntity.ok(ApiResponseDto.success(profile));
    }

    @PostMapping
    @Operation(summary = "Create mapping profile", description = "Create a new mapping profile for the organization")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponseDto<MappingProfileDTO>> createProfile(
            @Valid @RequestBody MappingProfileRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(required = false) Long organizationId) {

        Long orgId = resolveOrgId(currentUser, organizationId);
        MappingProfileDTO profile = mappingProfileService.createProfile(orgId, request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDto.success(profile));
    }

    @PutMapping("/{profileId}")
    @Operation(summary = "Update mapping profile", description = "Update an existing mapping profile")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponseDto<MappingProfileDTO>> updateProfile(
            @PathVariable String profileId,
            @Valid @RequestBody MappingProfileRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(required = false) Long organizationId) {

        Long orgId = resolveOrgId(currentUser, organizationId);
        MappingProfileDTO profile = mappingProfileService.updateProfile(profileId, orgId, request);
        return ResponseEntity.ok(ApiResponseDto.success(profile));
    }

    @DeleteMapping("/{profileId}")
    @Operation(summary = "Delete mapping profile", description = "Delete a mapping profile")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponseDto<Void>> deleteProfile(
            @PathVariable String profileId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(required = false) Long organizationId) {

        Long orgId = resolveOrgId(currentUser, organizationId);
        mappingProfileService.deleteProfile(profileId, orgId);
        return ResponseEntity.ok(ApiResponseDto.success(null));
    }

    @GetMapping("/default")
    @Operation(summary = "Get default profile", description = "Get the default mapping profile for the organization")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'USER')")
    public ResponseEntity<ApiResponseDto<MappingProfileDTO>> getDefaultProfile(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(required = false) Long organizationId) {

        Long orgId = resolveOrgId(currentUser, organizationId);
        MappingProfileDTO profile = mappingProfileService.getDefaultProfile(orgId);
        return ResponseEntity.ok(ApiResponseDto.success(profile));
    }

    /**
     * Resolve organization ID: super admins can specify org, others use their own.
     */
    private Long resolveOrgId(UserPrincipal currentUser, Long requestedOrgId) {
        if (requestedOrgId != null && currentUser.isSuperAdmin()) {
            return requestedOrgId;
        }
        Long orgId = currentUser.getOrganizationId();
        if (orgId == null) {
            throw new BadRequestException("User is not associated with an organization");
        }
        return orgId;
    }
}
