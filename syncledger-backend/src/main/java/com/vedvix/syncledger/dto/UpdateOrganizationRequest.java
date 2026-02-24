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

    // ERP Integration
    private String erpType;
    private String erpApiEndpoint;
    private String erpApiKey;
    private String erpTenantId;
    private String erpCompanyId;
    private Boolean erpAutoSync;
}
