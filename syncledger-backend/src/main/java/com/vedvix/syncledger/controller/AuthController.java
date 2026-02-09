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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller handling login, registration, and token management.
 * 
 * @author vedvix
 */
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "API endpoints for authentication, registration, and token management")
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
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(com.vedvix.syncledger.dto.ApiResponseDto.success("Login successful", response));
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh access token",
        description = "Refreshes the access token using a valid refresh token"
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
            @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request.getRefreshToken());
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
        description = "Logs out the user. Note: JWT tokens are stateless, so logout is primarily a client-side operation."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponse(
        responseCode = "200",
        description = "User logged out successfully"
    )
    public ResponseEntity<com.vedvix.syncledger.dto.ApiResponseDto<Void>> logout() {
        // JWT is stateless, so logout is handled client-side
        return ResponseEntity.ok(com.vedvix.syncledger.dto.ApiResponseDto.success("Logged out successfully"));
    }
}
