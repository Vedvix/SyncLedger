package com.vedvix.syncledger.dto;

import jakarta.validation.constraints.Email;
import lombok.*;

/**
 * Request DTO for updating an Organization.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrganizationRequest {
    private String name;
    
    @Email(message = "Invalid email address")
    private String emailAddress;
    
    private String status;
    private String sageApiEndpoint;
    private String sageApiKey;

    // Microsoft credentials
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
    private Boolean erpAutoSync;
}
