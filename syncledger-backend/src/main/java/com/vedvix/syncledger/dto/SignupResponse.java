package com.vedvix.syncledger.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Response DTO for organization signup.
 * Includes both org info and auth tokens.
 * 
 * @author vedvix
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupResponse {

    private OrganizationDTO organization;
    private SubscriptionDTO subscription;
    private AuthResponse auth;
    private String message;
}
