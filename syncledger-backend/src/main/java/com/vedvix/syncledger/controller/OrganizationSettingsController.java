package com.vedvix.syncledger.controller;

import com.vedvix.syncledger.dto.*;
import com.vedvix.syncledger.exception.BadRequestException;
import com.vedvix.syncledger.exception.ForbiddenException;
import com.vedvix.syncledger.exception.ResourceNotFoundException;
import com.vedvix.syncledger.model.Organization;
import com.vedvix.syncledger.repository.OrganizationRepository;
import com.vedvix.syncledger.security.UserPrincipal;
import com.vedvix.syncledger.service.EncryptionService;
import com.vedvix.syncledger.service.erp.ErpConnector;
import com.vedvix.syncledger.service.erp.ErpConnectorFactory;
import com.vedvix.syncledger.service.erp.ErpPropertyDefinitions;
import com.vedvix.syncledger.service.erp.ErpPropertyService;
import com.vedvix.syncledger.service.erp.ErpSyncResult;
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
import java.util.*;

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
    private final ErpConnectorFactory erpConnectorFactory;
    private final ErpPropertyService erpPropertyService;

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

    @GetMapping("/erp-types")
    @Operation(
        summary = "List available ERP types",
        description = "Returns all supported ERP types with display names, descriptions, and whether a connector is implemented"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponseDto<List<Map<String, Object>>>> getErpTypes() {
        List<Map<String, Object>> types = new ArrayList<>();
        for (ErpType et : ErpType.values()) {
            if (et == ErpType.NONE) continue;
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("type", et.name());
            info.put("displayName", et.getDisplayName());
            info.put("description", et.getDescription());
            info.put("implemented", erpConnectorFactory.hasConnector(et));
            info.put("propertyCount", ErpPropertyDefinitions.getDefinitions(et).size());
            types.add(info);
        }
        return ResponseEntity.ok(ApiResponseDto.success(types));
    }

    @GetMapping("/erp-types/{type}/properties")
    @Operation(
        summary = "Get property definitions for an ERP type",
        description = "Returns the property schema (fields, labels, types, required markers) so the frontend can render a dynamic config form"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponseDto<List<ErpPropertyDefinitions.PropertyDef>>> getErpTypeProperties(
            @PathVariable String type) {
        ErpType erpType = parseErpType(type);
        return ResponseEntity.ok(
                ApiResponseDto.success(ErpPropertyDefinitions.getDefinitions(erpType)));
    }

    @GetMapping("/erp-config")
    @Operation(
        summary = "Get ERP integration configuration",
        description = "Returns the current ERP integration settings for the organization (secrets masked)"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponseDto<ErpConfigDTO>> getErpConfig(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Organization org = resolveOrganization(currentUser);
        return ResponseEntity.ok(ApiResponseDto.success(buildErpConfigDTO(org)));
    }

    @PutMapping("/erp-config")
    @Operation(
        summary = "Update ERP integration configuration",
        description = "Updates ERP integration settings. Properties are generic key-value pairs; secrets are encrypted at rest."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponseDto<ErpConfigDTO>> updateErpConfig(
            @Valid @RequestBody UpdateErpConfigRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Organization org = resolveOrganization(currentUser);
        applyErpConfigUpdate(org, request, currentUser.getEmail());
        return ResponseEntity.ok(ApiResponseDto.success("ERP configuration updated successfully",
                buildErpConfigDTO(org)));
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

        applyErpConfigUpdate(org, request, currentUser.getEmail());
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

        ErpType erpType = org.getErpType();
        ErpConnector connector = erpConnectorFactory.getConnector(erpType);
        Map<String, String> properties = erpPropertyService
                .getDecryptedProperties(org.getId(), erpType);

        ErpSyncResult result = connector.testConnection(properties);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("erpType", erpType.name());
        data.put("erpTypeDisplayName", erpType.getDisplayName());
        data.put("connected", result.success());
        data.put("httpStatus", result.httpStatusCode());

        if (result.success()) {
            data.put("message", "Successfully connected to " + erpType.getDisplayName());
        } else {
            data.put("errorCode", result.errorCode());
            data.put("errorMessage", result.errorMessage());
            String guidance = getErpErrorGuidance(erpType, result.errorCode());
            if (guidance != null) data.put("guidance", guidance);
        }

        log.info("ERP verify for org {}: type={}, connected={}, user={}",
                org.getName(), erpType, result.success(), userEmail);

        if (result.success()) {
            return ApiResponseDto.success("ERP connection verified successfully", data);
        } else {
            return ApiResponseDto.<Map<String, Object>>builder()
                    .success(false)
                    .message("ERP connection failed")
                    .data(data)
                    .timestamp(java.time.LocalDateTime.now())
                    .build();
        }
    }

    private String getErpErrorGuidance(ErpType erpType, String errorCode) {
        if (errorCode == null) return null;
        if (erpType == ErpType.SAGE) {
            return switch (errorCode) {
                case "GW-0011" -> "The Sage Intacct gateway rejected the Sender ID. "
                        + "Ensure the Sender ID is correct and authorized in the target company's Sage Intacct instance: "
                        + "Company → Admin → Web Services Authorizations → add your Sender ID. "
                        + "If you don't have a Sender ID, register at https://developer.intacct.com";
                case "XL03000006" -> "Invalid login credentials. Verify your User ID, Company ID, and Password in ERP settings. "
                        + "Note: Sender ID/Password and User ID/Password are separate — check both.";
                case "XL03000009" -> "User account is locked or inactive in Sage Intacct.";
                case "CONNECTION_ERROR" -> "Could not reach the Sage Intacct gateway. Check the API endpoint URL and network connectivity.";
                default -> null;
            };
        }
        return null;
    }

    // ==================== Private Helpers ====================

    /**
     * Apply an ERP config update: set the erpType on the Organization and save properties.
     */
    private void applyErpConfigUpdate(Organization org, UpdateErpConfigRequest request, String userEmail) {
        if (request.getErpType() != null) {
            ErpType newType = parseErpType(request.getErpType());
            ErpType oldType = org.getErpType();

            // If switching ERP type, clear old properties
            if (oldType != null && oldType != newType && oldType != ErpType.NONE) {
                erpPropertyService.deleteProperties(org.getId(), oldType);
                log.info("Cleared {} properties for org {} (switching to {})",
                        oldType, org.getName(), newType);
            }

            org.setErpType(newType);
            organizationRepository.save(org);
        }

        // Save generic properties
        if (request.getProperties() != null && !request.getProperties().isEmpty()
                && org.getErpType() != null && org.getErpType() != ErpType.NONE) {
            erpPropertyService.saveProperties(org.getId(), org.getErpType(), request.getProperties());
        }

        log.info("ERP config updated for org: {} by user: {}", org.getName(), userEmail);
    }

    private ErpType parseErpType(String type) {
        try {
            return ErpType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid ERP type: " + type
                    + ". Valid types: " + Arrays.toString(ErpType.values()));
        }
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
        ErpType erpType = org.getErpType() != null ? org.getErpType() : ErpType.NONE;
        boolean configured = erpType != ErpType.NONE;

        Map<String, String> maskedProps = Collections.emptyMap();
        if (configured) {
            maskedProps = erpPropertyService.getMaskedProperties(org.getId(), erpType);
            // If we have properties AND required ones are populated, mark configured
            List<String> missing = erpPropertyService.validateRequired(org.getId(), erpType);
            configured = missing.isEmpty();
        }

        return ErpConfigDTO.builder()
                .erpType(erpType.name())
                .erpTypeDisplayName(erpType.getDisplayName())
                .erpConfigured(configured)
                .properties(maskedProps)
                .propertyDefinitions(ErpPropertyDefinitions.getDefinitions(erpType))
                .build();
    }
}
