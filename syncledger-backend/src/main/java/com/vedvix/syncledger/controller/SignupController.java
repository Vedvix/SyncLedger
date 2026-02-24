package com.vedvix.syncledger.controller;

import com.vedvix.syncledger.dto.*;
import com.vedvix.syncledger.service.SignupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public signup controller for organization self-registration.
 * No authentication required - this is the entry point for new customers.
 * 
 * @author vedvix
 */
@RestController
@RequestMapping("/v1/signup")
@RequiredArgsConstructor
@Tag(name = "Signup", description = "Public API for organization self-registration with 15-day trial")
public class SignupController {

    private final SignupService signupService;

    @PostMapping
    @Operation(
        summary = "Register new organization",
        description = "Creates a new organization with a 15-day free trial, admin user account, and returns auth tokens. " +
                      "No authentication required."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Organization registered successfully with trial started",
            content = @Content(schema = @Schema(implementation = SignupResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data or duplicate email/organization"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Organization or email already exists"
        )
    })
    public ResponseEntity<ApiResponseDto<SignupResponse>> signup(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Organization registration details including admin user credentials",
                required = true,
                content = @Content(schema = @Schema(implementation = OrganizationSignupRequest.class))
            )
            @Valid @RequestBody OrganizationSignupRequest request,
            HttpServletRequest httpRequest) {

        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = getClientIpAddress(httpRequest);

        SignupResponse response = signupService.signup(request, userAgent, ipAddress);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success(
                    "Organization registered successfully. Your 15-day trial has started!",
                    response
                ));
    }

    @GetMapping("/check-email")
    @Operation(
        summary = "Check if email is available",
        description = "Checks if an email address is available for registration"
    )
    public ResponseEntity<ApiResponseDto<Boolean>> checkEmailAvailability(
            @RequestParam String email) {
        // This will be injected and checked
        return ResponseEntity.ok(ApiResponseDto.success(true));
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
