package com.vedvix.syncledger.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

/**
 * Request DTO for creating a new Organization.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrganizationRequest {
    
    @NotBlank(message = "Organization name is required")
    private String name;

    @NotBlank(message = "Organization slug is required")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must contain only lowercase letters, numbers, and hyphens")
    private String slug;

    @Email(message = "Invalid email address")
    private String emailAddress;

    private String sageApiEndpoint;
    private String sageApiKey;

    // Microsoft Graph API Credentials
    private String msClientId;
    private String msClientSecret;
    private String msTenantId;

    @Email(message = "Invalid mailbox email address")
    private String msMailboxEmail;

    // ERP Integration
    private String erpType;
    private String erpApiEndpoint;
    private String erpApiKey;
    private String erpTenantId;
    private String erpCompanyId;
}
