package com.vedvix.syncledger.dto;

import lombok.*;

/**
 * Request DTO for logout operation.
 * 
 * @author vedvix
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogoutRequest {
    /**
     * The refresh token to revoke.
     * Optional - if not provided, logout will still succeed
     * but the refresh token won't be invalidated server-side.
     */
    private String refreshToken;
}
