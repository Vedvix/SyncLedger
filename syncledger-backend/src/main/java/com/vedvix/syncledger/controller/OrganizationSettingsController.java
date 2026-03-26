package com.vedvix.syncledger.controller;

import com.vedvix.syncledger.dto.*;
import com.vedvix.syncledger.exception.BadRequestException;
import com.vedvix.syncledger.exception.ForbiddenException;
import com.vedvix.syncledger.exception.ResourceNotFoundException;
import com.vedvix.syncledger.exception.UnauthorizedException;
import com.vedvix.syncledger.model.Organization;
import com.vedvix.syncledger.repository.OrganizationRepository;
import com.vedvix.syncledger.security.UserPrincipal;
import com.vedvix.syncledger.service.EncryptionService;
import com.vedvix.syncledger.service.SageIntacctService;
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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

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
    private final SageIntacctService sageIntacctService;

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
        description = "Tests the configured Azure AD credentials by attempting to authenticate with Microsoft Graph API. " +
                       "If a request body with msClientSecret is provided, that raw secret is used directly (bypassing encryption round-trip)."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Verification result"),
        @ApiResponse(responseCode = "400", description = "Credentials not configured")
    })
    public ResponseEntity<ApiResponseDto<MicrosoftConfigDTO>> verifyMicrosoftConfig(
            @RequestBody(required = false) UpdateMicrosoftConfigRequest rawRequest,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Organization org = resolveOrganization(currentUser);

        // Determine which credentials to use
        String clientId;
        String clientSecret;
        String tenantId;

        if (rawRequest != null && rawRequest.getMsClientSecret() != null && !rawRequest.getMsClientSecret().isBlank()) {
            // Use raw credentials from request body (bypasses encryption round-trip)
            clientId = rawRequest.getMsClientId() != null ? rawRequest.getMsClientId() : org.getMsClientId();
            clientSecret = rawRequest.getMsClientSecret();
            tenantId = rawRequest.getMsTenantId() != null ? rawRequest.getMsTenantId() : org.getMsTenantId();
            log.info("Verify using RAW credentials from request body for org {}", org.getName());
        } else {
            // Use stored encrypted credentials
            if (org.getMsClientId() == null || org.getMsClientSecretEncrypted() == null || org.getMsTenantId() == null) {
                throw new BadRequestException("Microsoft credentials are not configured. Please update them first.");
            }
            clientId = org.getMsClientId();
            clientSecret = encryptionService.decrypt(org.getMsClientSecretEncrypted());
            tenantId = org.getMsTenantId();
            log.info("Verify using STORED (encrypted) credentials for org {}", org.getName());
        }

        log.info("Verify attempt for org {}: clientId={}, tenantId={}, secretLength={}, secretPrefix={}",
                org.getName(), clientId, tenantId,
                clientSecret != null ? clientSecret.length() : 0,
                clientSecret != null && clientSecret.length() >= 5 ? clientSecret.substring(0, 5) + "..." : "null");

        try {
            com.azure.identity.ClientSecretCredential credential =
                new com.azure.identity.ClientSecretCredentialBuilder()
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .tenantId(tenantId)
                    .build();

            com.azure.core.credential.TokenRequestContext context =
                new com.azure.core.credential.TokenRequestContext();
            context.addScopes("https://graph.microsoft.com/.default");

            // This will throw if credentials are invalid
            credential.getToken(context).block();

            // Mark as verified and save the working credentials
            if (rawRequest != null && rawRequest.getMsClientSecret() != null && !rawRequest.getMsClientSecret().isBlank()) {
                org.setMsClientId(clientId);
                org.setMsClientSecretEncrypted(encryptionService.encrypt(clientSecret));
                org.setMsTenantId(tenantId);
                if (rawRequest.getMsMailboxEmail() != null) {
                    org.setMsMailboxEmail(rawRequest.getMsMailboxEmail());
                    org.setEmailAddress(rawRequest.getMsMailboxEmail());
                }
            }
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

    // ==================== Super Admin: Manage any org's ERP config ====================

    @GetMapping("/admin/{organizationId}/erp-config")
    @Operation(
        summary = "Get ERP config for any org (Super Admin)",
        description = "Super Admin can view ERP configuration for any organization"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponseDto<ErpConfigDTO>> getErpConfigForOrg(
            @PathVariable Long organizationId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));

        return ResponseEntity.ok(ApiResponseDto.success(buildErpConfigDTO(org)));
    }

    @PutMapping("/admin/{organizationId}/erp-config")
    @Operation(
        summary = "Update ERP config for any org (Super Admin)",
        description = "Super Admin can update ERP integration settings for any organization"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponseDto<ErpConfigDTO>> updateErpConfigForOrg(
            @PathVariable Long organizationId,
            @Valid @RequestBody UpdateErpConfigRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));

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

        log.info("Super Admin updated ERP config for org: {} by: {}",
                org.getName(), currentUser.getEmail());

        return ResponseEntity.ok(ApiResponseDto.success("ERP configuration updated", buildErpConfigDTO(org)));
    }

    // ==================== ERP Connection Verification ====================

    @PostMapping("/erp-config/verify")
    @Operation(
        summary = "Verify ERP connection",
        description = "Tests the configured ERP credentials by attempting to authenticate with the ERP API."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> verifyErpConfig(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Organization org = resolveOrganization(currentUser);
        return ResponseEntity.ok(doVerifyErpConfig(org, currentUser.getEmail()));
    }

    @PostMapping("/admin/{organizationId}/erp-config/verify")
    @Operation(
        summary = "Verify ERP connection for any org (Super Admin)",
        description = "Super Admin can test ERP credentials for any organization."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> verifyErpConfigForOrg(
            @PathVariable Long organizationId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));
        return ResponseEntity.ok(doVerifyErpConfig(org, currentUser.getEmail()));
    }

    private ApiResponseDto<Map<String, Object>> doVerifyErpConfig(Organization org, String userEmail) {
        if (org.getErpType() == null || org.getErpType() == ErpType.NONE) {
            throw new BadRequestException("No ERP integration configured. Please set up ERP settings first.");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("erpType", org.getErpType().name());
        result.put("companyId", org.getErpCompanyId());
        result.put("userId", org.getErpTenantId());

        if (org.getErpType() == ErpType.SAGE) {
            SageIntacctService.SageResponse response = sageIntacctService.testConnection(org);
            result.put("connected", response.success());
            result.put("httpStatus", response.httpStatusCode());
            if (response.success()) {
                result.put("message", "Successfully connected to Sage Intacct");
            } else {
                result.put("errorCode", response.errorCode());
                result.put("errorMessage", response.errorMessage());
                String guidance = getSageErrorGuidance(response.errorCode());
                if (guidance != null) result.put("guidance", guidance);
            }
        } else {
            throw new BadRequestException(org.getErpType().name() + " verification is not yet implemented.");
        }

        boolean connected = Boolean.TRUE.equals(result.get("connected"));
        log.info("ERP verify for org {}: connected={}, user={}", org.getName(), connected, userEmail);

        if (connected) {
            return ApiResponseDto.success("ERP connection verified successfully", result);
        } else {
            return ApiResponseDto.<Map<String, Object>>builder()
                    .success(false)
                    .message("ERP connection failed")
                    .data(result)
                    .timestamp(java.time.LocalDateTime.now())
                    .build();
        }
    }

    private String getSageErrorGuidance(String errorCode) {
        if (errorCode == null) return null;
        return switch (errorCode) {
            case "GW-0011" -> "The Sage Intacct gateway rejected the request. "
                    + "The User ID in ERP settings must be authorized as a Web Services sender "
                    + "in your Sage Intacct company (Company > Setup > Company > Security > Web Services authorizations).";
            case "XL03000006" -> "Invalid login credentials. Verify your User ID, Company ID, and Password in ERP settings.";
            case "XL03000009" -> "User account is locked or inactive in Sage Intacct.";
            default -> null;
        };
    }

    /**
     * Resolve the organization from the current user context.
     */
    private Organization resolveOrganization(UserPrincipal currentUser) {
        Long orgId = currentUser.getOrganizationId();
        if (orgId != null) {
            return organizationRepository.findById(orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", orgId));
        }

        if (currentUser.isSuperAdmin()) {
            Long requestedOrgId = resolveRequestedOrganizationId();
            if (requestedOrgId == null) {
                throw new BadRequestException("For Platform Admin, provide organization context via query param 'organizationId' or header 'X-Organization-Id'.");
            }

            return organizationRepository.findById(requestedOrgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", requestedOrgId));
        }

        throw new ForbiddenException("User is not associated with an organization");
    }

    private Long resolveRequestedOrganizationId() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }

        HttpServletRequest request = attrs.getRequest();
        if (request == null) {
            return null;
        }

        String param = request.getParameter("organizationId");
        Long fromParam = parseOrganizationId(param);
        if (fromParam != null) {
            return fromParam;
        }

        String header = request.getHeader("X-Organization-Id");
        return parseOrganizationId(header);
    }

    private Long parseOrganizationId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
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
