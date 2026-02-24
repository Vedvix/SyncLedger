package com.vedvix.syncledger.controller;

import com.vedvix.syncledger.dto.*;
import com.vedvix.syncledger.exception.BadRequestException;
import com.vedvix.syncledger.exception.ResourceNotFoundException;
import com.vedvix.syncledger.exception.UnauthorizedException;
import com.vedvix.syncledger.model.Organization;
import com.vedvix.syncledger.repository.OrganizationRepository;
import com.vedvix.syncledger.security.UserPrincipal;
import com.vedvix.syncledger.service.EncryptionService;
import com.vedvix.syncledger.model.ErpType;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Organization settings controller for managing Microsoft credentials
 * and other organization-level configuration.
 * 
 * Accessible by Org Admins (for their own org) and Super Admins (for any org).
 * 
 * @author vedvix
 */
@Slf4j
@RestController
@RequestMapping("/v1/organization-settings")
@RequiredArgsConstructor
@Tag(name = "Organization Settings", description = "API for managing organization integration settings")
public class OrganizationSettingsController {

    private final OrganizationRepository organizationRepository;
    private final EncryptionService encryptionService;

    @GetMapping("/microsoft-config")
    @Operation(
        summary = "Get Microsoft Graph configuration",
        description = "Returns the current Microsoft Graph API configuration for the organization (secrets masked)"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @ApiResponse(responseCode = "200", description = "Configuration returned")
    public ResponseEntity<ApiResponseDto<MicrosoftConfigDTO>> getMicrosoftConfig(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Organization org = resolveOrganization(currentUser);

        MicrosoftConfigDTO config = MicrosoftConfigDTO.builder()
                .msClientId(org.getMsClientId())
                .msClientSecretMasked(org.getMsClientSecretEncrypted() != null ?
                        EncryptionService.maskSecret(encryptionService.decrypt(org.getMsClientSecretEncrypted())) : null)
                .msTenantId(org.getMsTenantId())
                .msMailboxEmail(org.getMsMailboxEmail())
                .msCredentialsVerified(org.getMsCredentialsVerified())
                .msCredentialsVerifiedAt(org.getMsCredentialsVerifiedAt())
                .build();

        return ResponseEntity.ok(ApiResponseDto.success(config));
    }

    @PutMapping("/microsoft-config")
    @Operation(
        summary = "Update Microsoft Graph configuration",
        description = "Updates Azure AD credentials for email integration. The client secret is encrypted at rest using AES-256-GCM."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Configuration updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid configuration data"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<ApiResponseDto<MicrosoftConfigDTO>> updateMicrosoftConfig(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Microsoft Graph API credentials",
                required = true,
                content = @Content(schema = @Schema(implementation = UpdateMicrosoftConfigRequest.class))
            )
            @Valid @RequestBody UpdateMicrosoftConfigRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Organization org = resolveOrganization(currentUser);

        // Update credentials
        org.setMsClientId(request.getMsClientId());
        org.setMsClientSecretEncrypted(encryptionService.encrypt(request.getMsClientSecret()));
        org.setMsTenantId(request.getMsTenantId());
        org.setMsMailboxEmail(request.getMsMailboxEmail());
        org.setMsCredentialsVerified(false); // Reset verification on update
        org.setMsCredentialsVerifiedAt(null);

        // Also update the organization's email address if mailbox is set
        if (request.getMsMailboxEmail() != null) {
            org.setEmailAddress(request.getMsMailboxEmail());
        }

        organizationRepository.save(org);

        log.info("Microsoft config updated for org: {} by user: {}",
                org.getName(), currentUser.getEmail());

        MicrosoftConfigDTO config = MicrosoftConfigDTO.builder()
                .msClientId(org.getMsClientId())
                .msClientSecretMasked(EncryptionService.maskSecret(request.getMsClientSecret()))
                .msTenantId(org.getMsTenantId())
                .msMailboxEmail(org.getMsMailboxEmail())
                .msCredentialsVerified(false)
                .build();

        return ResponseEntity.ok(ApiResponseDto.success("Microsoft configuration updated successfully", config));
    }

    @PostMapping("/microsoft-config/verify")
    @Operation(
        summary = "Verify Microsoft Graph credentials",
        description = "Tests the configured Azure AD credentials by attempting to authenticate with Microsoft Graph API"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Verification result"),
        @ApiResponse(responseCode = "400", description = "Credentials not configured")
    })
    public ResponseEntity<ApiResponseDto<MicrosoftConfigDTO>> verifyMicrosoftConfig(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Organization org = resolveOrganization(currentUser);

        if (org.getMsClientId() == null || org.getMsClientSecretEncrypted() == null || org.getMsTenantId() == null) {
            throw new BadRequestException("Microsoft credentials are not configured. Please update them first.");
        }

        try {
            // Attempt to get an access token to verify credentials
            String clientSecret = encryptionService.decrypt(org.getMsClientSecretEncrypted());

            com.azure.identity.ClientSecretCredential credential =
                new com.azure.identity.ClientSecretCredentialBuilder()
                    .clientId(org.getMsClientId())
                    .clientSecret(clientSecret)
                    .tenantId(org.getMsTenantId())
                    .build();

            com.azure.core.credential.TokenRequestContext context =
                new com.azure.core.credential.TokenRequestContext();
            context.addScopes("https://graph.microsoft.com/.default");

            // This will throw if credentials are invalid
            credential.getToken(context).block();

            // Mark as verified
            org.setMsCredentialsVerified(true);
            org.setMsCredentialsVerifiedAt(LocalDateTime.now());
            organizationRepository.save(org);

            log.info("Microsoft credentials verified successfully for org: {}", org.getName());

            MicrosoftConfigDTO config = MicrosoftConfigDTO.builder()
                    .msClientId(org.getMsClientId())
                    .msClientSecretMasked(EncryptionService.maskSecret(clientSecret))
                    .msTenantId(org.getMsTenantId())
                    .msMailboxEmail(org.getMsMailboxEmail())
                    .msCredentialsVerified(true)
                    .msCredentialsVerifiedAt(org.getMsCredentialsVerifiedAt())
                    .build();

            return ResponseEntity.ok(ApiResponseDto.success("Microsoft credentials verified successfully!", config));

        } catch (Exception e) {
            log.warn("Microsoft credential verification failed for org {}: {}", org.getName(), e.getMessage());

            org.setMsCredentialsVerified(false);
            organizationRepository.save(org);

            return ResponseEntity.ok(ApiResponseDto.error(
                    "Credential verification failed: " + e.getMessage()));
        }
    }

    // ==================== ERP Configuration ====================

    @GetMapping("/erp-config")
    @Operation(
        summary = "Get ERP integration configuration",
        description = "Returns the current ERP integration settings for the organization (API key masked)"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponseDto<ErpConfigDTO>> getErpConfig(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Organization org = resolveOrganization(currentUser);

        ErpConfigDTO dto = buildErpConfigDTO(org);
        return ResponseEntity.ok(ApiResponseDto.success(dto));
    }

    @PutMapping("/erp-config")
    @Operation(
        summary = "Update ERP integration configuration",
        description = "Updates ERP integration settings for the organization. API key is encrypted at rest."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponseDto<ErpConfigDTO>> updateErpConfig(
            @Valid @RequestBody UpdateErpConfigRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Organization org = resolveOrganization(currentUser);

        if (request.getErpType() != null) {
            try {
                org.setErpType(ErpType.valueOf(request.getErpType().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid ERP type: " + request.getErpType());
            }
        }
        if (request.getErpApiEndpoint() != null) {
            org.setErpApiEndpoint(request.getErpApiEndpoint());
        }
        if (request.getErpApiKey() != null && !request.getErpApiKey().isBlank()) {
            org.setErpApiKeyEncrypted(encryptionService.encrypt(request.getErpApiKey()));
        }
        if (request.getErpTenantId() != null) {
            org.setErpTenantId(request.getErpTenantId());
        }
        if (request.getErpCompanyId() != null) {
            org.setErpCompanyId(request.getErpCompanyId());
        }
        if (request.getErpAutoSync() != null) {
            org.setErpAutoSync(request.getErpAutoSync());
        }

        organizationRepository.save(org);

        log.info("ERP config updated for org: {} by user: {}", org.getName(), currentUser.getEmail());

        ErpConfigDTO dto = buildErpConfigDTO(org);
        return ResponseEntity.ok(ApiResponseDto.success("ERP configuration updated successfully", dto));
    }

    // ==================== Onboarding Completion ====================

    @PostMapping("/complete-onboarding")
    @Operation(
        summary = "Mark onboarding as complete",
        description = "Transitions organization status from ONBOARDING to TRIAL (or ACTIVE if subscription active)"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponseDto<String>> completeOnboarding(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Organization org = resolveOrganization(currentUser);

        if (org.getMsClientId() == null || org.getMsTenantId() == null || org.getMsMailboxEmail() == null) {
            throw new BadRequestException("Microsoft email credentials must be configured before completing onboarding");
        }

        if (org.getStatus() == com.vedvix.syncledger.model.OrganizationStatus.ONBOARDING) {
            var sub = org.getSubscription();
            if (sub != null && sub.getStatus() == com.vedvix.syncledger.model.SubscriptionStatus.ACTIVE) {
                org.setStatus(com.vedvix.syncledger.model.OrganizationStatus.ACTIVE);
            } else {
                org.setStatus(com.vedvix.syncledger.model.OrganizationStatus.TRIAL);
            }
            organizationRepository.save(org);
            log.info("Onboarding completed for org: {}", org.getName());
        }

        return ResponseEntity.ok(ApiResponseDto.success("Onboarding completed successfully"));
    }

    // ==================== Super Admin: Manage any org's config ====================

    @PutMapping("/admin/{organizationId}/microsoft-config")
    @Operation(
        summary = "Update Microsoft config for any org (Super Admin)",
        description = "Super Admin can update Microsoft credentials for any organization"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponseDto<MicrosoftConfigDTO>> updateMicrosoftConfigForOrg(
            @PathVariable Long organizationId,
            @Valid @RequestBody UpdateMicrosoftConfigRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));

        org.setMsClientId(request.getMsClientId());
        org.setMsClientSecretEncrypted(encryptionService.encrypt(request.getMsClientSecret()));
        org.setMsTenantId(request.getMsTenantId());
        org.setMsMailboxEmail(request.getMsMailboxEmail());
        org.setMsCredentialsVerified(false);
        org.setMsCredentialsVerifiedAt(null);

        if (request.getMsMailboxEmail() != null) {
            org.setEmailAddress(request.getMsMailboxEmail());
        }

        organizationRepository.save(org);

        log.info("Super Admin updated Microsoft config for org: {} by: {}",
                org.getName(), currentUser.getEmail());

        MicrosoftConfigDTO config = MicrosoftConfigDTO.builder()
                .msClientId(org.getMsClientId())
                .msClientSecretMasked(EncryptionService.maskSecret(request.getMsClientSecret()))
                .msTenantId(org.getMsTenantId())
                .msMailboxEmail(org.getMsMailboxEmail())
                .msCredentialsVerified(false)
                .build();

        return ResponseEntity.ok(ApiResponseDto.success("Microsoft configuration updated", config));
    }

    /**
     * Resolve the organization from the current user context.
     */
    private Organization resolveOrganization(UserPrincipal currentUser) {
        Long orgId = currentUser.getOrganizationId();
        if (orgId == null) {
            throw new UnauthorizedException("Super Admin must use admin-specific endpoints for org management");
        }
        return organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", orgId));
    }

    private ErpConfigDTO buildErpConfigDTO(Organization org) {
        String maskedKey = null;
        if (org.getErpApiKeyEncrypted() != null) {
            maskedKey = EncryptionService.maskSecret(encryptionService.decrypt(org.getErpApiKeyEncrypted()));
        }
        boolean configured = org.getErpType() != null && org.getErpType() != ErpType.NONE
                && org.getErpApiEndpoint() != null && !org.getErpApiEndpoint().isBlank();

        return ErpConfigDTO.builder()
                .erpType(org.getErpType() != null ? org.getErpType().name() : "NONE")
                .erpApiEndpoint(org.getErpApiEndpoint())
                .erpApiKeyMasked(maskedKey)
                .erpTenantId(org.getErpTenantId())
                .erpCompanyId(org.getErpCompanyId())
                .erpAutoSync(org.getErpAutoSync())
                .erpConfigured(configured)
                .build();
    }
}
