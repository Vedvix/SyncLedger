package com.vedvix.syncledger.controller;

import com.vedvix.syncledger.dto.*;
import com.vedvix.syncledger.security.UserPrincipal;
import com.vedvix.syncledger.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Authentication Controller handling login, registration, token management, and session management.
 * 
 * Features:
 * - Secure login with refresh token rotation
 * - Session management (view and revoke sessions)
 * - Multi-device support
 * 
 * @author vedvix
 */
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "API endpoints for authentication, registration, token and session management")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(
        summary = "Login user",
        description = "Authenticates a user with email and password. Returns access and refresh tokens.",
        tags = {"Authentication"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Login successful",
            content = @Content(schema = @Schema(implementation = com.vedvix.syncledger.dto.ApiResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid email or password",
            content = @Content(schema = @Schema(implementation = com.vedvix.syncledger.dto.ApiResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication failed",
            content = @Content(schema = @Schema(implementation = com.vedvix.syncledger.dto.ApiResponseDto.class))
        )
    })
    public ResponseEntity<com.vedvix.syncledger.dto.ApiResponseDto<AuthResponse>> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Login credentials",
                required = true,
                content = @Content(schema = @Schema(implementation = LoginRequest.class))
            )
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = getClientIpAddress(httpRequest);
        AuthResponse response = authService.login(request, userAgent, ipAddress);
        return ResponseEntity.ok(com.vedvix.syncledger.dto.ApiResponseDto.success("Login successful", response));
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh access token",
        description = "Refreshes the access token using a valid refresh token. Implements token rotation for security."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Token refreshed successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid or expired refresh token"
        )
    })
    public ResponseEntity<com.vedvix.syncledger.dto.ApiResponseDto<AuthResponse>> refreshToken(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Refresh token request",
                required = true,
                content = @Content(schema = @Schema(implementation = RefreshTokenRequest.class))
            )
            @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = getClientIpAddress(httpRequest);
        AuthResponse response = authService.refreshToken(request.getRefreshToken(), userAgent, ipAddress);
        return ResponseEntity.ok(com.vedvix.syncledger.dto.ApiResponseDto.success("Token refreshed", response));
    }

    @PostMapping("/register-super-admin")
    @Operation(
        summary = "Register super admin",
        description = "Registers the first super admin user. Only works if no users exist in the system."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Super admin registered successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Users already exist or invalid request"
        )
    })
    public ResponseEntity<com.vedvix.syncledger.dto.ApiResponseDto<AuthResponse>> registerSuperAdmin(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Super admin registration details",
                required = true,
                content = @Content(schema = @Schema(implementation = CreateUserRequest.class))
            )
            @Valid @RequestBody CreateUserRequest request) {
        AuthResponse response = authService.registerSuperAdmin(request);
        return ResponseEntity.ok(com.vedvix.syncledger.dto.ApiResponseDto.success("Super Admin registered successfully", response));
    }

    @GetMapping("/me")
    @Operation(
        summary = "Get current user info",
        description = "Returns detailed information about the currently authenticated user"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponse(
        responseCode = "200",
        description = "Current user information retrieved successfully"
    )
    public ResponseEntity<com.vedvix.syncledger.dto.ApiResponseDto<UserDTO>> getCurrentUser(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {
        UserDTO user = authService.getCurrentUser(currentUser);
        return ResponseEntity.ok(com.vedvix.syncledger.dto.ApiResponseDto.success(user));
    }

    @PostMapping("/change-password")
    @Operation(
        summary = "Change user password",
        description = "Allows authenticated user to change their password"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Password changed successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Current password incorrect or new password invalid"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - user not authenticated"
        )
    })
    public ResponseEntity<com.vedvix.syncledger.dto.ApiResponseDto<Void>> changePassword(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Password change request",
                required = true,
                content = @Content(schema = @Schema(implementation = ChangePasswordRequest.class))
            )
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(currentUser, request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(com.vedvix.syncledger.dto.ApiResponseDto.success("Password changed successfully"));
    }

    @PostMapping("/logout")
    @Operation(
        summary = "Logout user",
        description = "Logs out the user by revoking the refresh token."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponse(
        responseCode = "200",
        description = "User logged out successfully"
    )
    public ResponseEntity<com.vedvix.syncledger.dto.ApiResponseDto<Void>> logout(
            @RequestBody(required = false) LogoutRequest request) {
        String refreshToken = request != null ? request.getRefreshToken() : null;
        authService.logout(refreshToken);
        return ResponseEntity.ok(com.vedvix.syncledger.dto.ApiResponseDto.success("Logged out successfully"));
    }

    @GetMapping("/sessions")
    @Operation(
        summary = "Get active sessions",
        description = "Returns all active sessions (devices) for the current user"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponse(
        responseCode = "200",
        description = "Active sessions retrieved successfully"
    )
    public ResponseEntity<com.vedvix.syncledger.dto.ApiResponseDto<List<SessionDTO>>> getActiveSessions(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<SessionDTO> sessions = authService.getActiveSessions(currentUser.getId(), null);
        return ResponseEntity.ok(com.vedvix.syncledger.dto.ApiResponseDto.success(sessions));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(
        summary = "Revoke a session",
        description = "Revokes a specific session (logs out from a specific device)"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Session revoked successfully"),
        @ApiResponse(responseCode = "404", description = "Session not found"),
        @ApiResponse(responseCode = "403", description = "Not authorized to revoke this session")
    })
    public ResponseEntity<com.vedvix.syncledger.dto.ApiResponseDto<Void>> revokeSession(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long sessionId) {
        authService.revokeSession(currentUser.getId(), sessionId);
        return ResponseEntity.ok(com.vedvix.syncledger.dto.ApiResponseDto.success("Session revoked successfully"));
    }

    @PostMapping("/logout-all")
    @Operation(
        summary = "Logout from all devices",
        description = "Revokes all refresh tokens for the user, logging them out from all devices"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponse(
        responseCode = "200",
        description = "Logged out from all devices successfully"
    )
    public ResponseEntity<com.vedvix.syncledger.dto.ApiResponseDto<Map<String, Integer>>> logoutAllDevices(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {
        int count = authService.logoutAllDevices(currentUser.getId());
        return ResponseEntity.ok(com.vedvix.syncledger.dto.ApiResponseDto.success(
                "Logged out from all devices",
                Map.of("sessionsRevoked", count)
        ));
    }

    /**
     * Extract real client IP address, handling proxies and load balancers.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_CLIENT_IP"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For can contain multiple IPs, take the first one
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }
}
