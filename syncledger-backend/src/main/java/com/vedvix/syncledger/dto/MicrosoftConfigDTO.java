package com.vedvix.syncledger.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO for Microsoft Graph configuration display (secrets masked).
 * 
 * @author vedvix
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MicrosoftConfigDTO {

    private String msClientId;
    private String msClientSecretMasked;
    private String msTenantId;
    private String msMailboxEmail;
    private Boolean msCredentialsVerified;
    private LocalDateTime msCredentialsVerifiedAt;
}
