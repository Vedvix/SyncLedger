package com.vedvix.syncledger.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Request DTO for updating organization Microsoft Graph credentials.
 * These credentials allow per-org email mailbox access.
 * 
 * @author vedvix
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMicrosoftConfigRequest {

    @NotBlank(message = "Azure AD Client ID is required")
    private String msClientId;

    @NotBlank(message = "Azure AD Client Secret is required")
    private String msClientSecret;

    @NotBlank(message = "Azure AD Tenant ID is required")
    private String msTenantId;

    @NotBlank(message = "Mailbox email is required")
    @Email(message = "Invalid mailbox email address")
    private String msMailboxEmail;
}
