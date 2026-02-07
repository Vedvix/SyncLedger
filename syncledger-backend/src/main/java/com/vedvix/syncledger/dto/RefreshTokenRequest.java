package com.vedvix.syncledger.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Request DTO for refresh token.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
