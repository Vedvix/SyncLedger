package com.vedvix.syncledger.controller;

import com.vedvix.syncledger.dto.*;
import com.vedvix.syncledger.security.UserPrincipal;
import com.vedvix.syncledger.service.UserService;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * User Controller with multi-tenant support.
 * Organization-scoped access is enforced at service layer.
 * 
 * @author vedvix
 */
@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "API endpoints for user management within organization")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(
        summary = "Get organizational users",
        description = "Retrieves a paginated list of users in the authenticated user's organization. Only accessible to SUPER_ADMIN and ADMIN roles."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Users retrieved successfully",
            content = @Content(schema = @Schema(implementation = com.vedvix.syncledger.dto.ApiResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - user not authenticated"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - insufficient permissions"
        )
    })
    public ResponseEntity<ApiResponseDto<PagedResponse<UserDTO>>> getUsers(
            @Parameter(description = "Pagination information (page, size, sort)", required = false)
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @Parameter(description = "Search query to filter users by name or email")
            @RequestParam(required = false) String search,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {
        PagedResponse<UserDTO> users = userService.getUsers(pageable, search, currentUser);
        return ResponseEntity.ok(ApiResponseDto.success(users));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(
        summary = "Get user by ID",
        description = "Retrieves detailed information about a specific user in the organization"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User found and returned successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - user not authenticated"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - user not in your organization or insufficient permissions"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found"
        )
    })
    public ResponseEntity<ApiResponseDto<UserDTO>> getUser(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long id,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {
        UserDTO user = userService.getUserById(id, currentUser);
        return ResponseEntity.ok(ApiResponseDto.success(user));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(
        summary = "Create new user",
        description = "Creates a new user within the authenticated user's organization. Only SUPER_ADMIN and ADMIN can create users."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "User created successfully",
            content = @Content(schema = @Schema(implementation = UserDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input data - email already exists or invalid fields"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - user not authenticated"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - insufficient permissions"
        )
    })
    public ResponseEntity<ApiResponseDto<UserDTO>> createUser(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "User creation details",
                required = true,
                content = @Content(schema = @Schema(implementation = CreateUserRequest.class))
            )
            @Valid @RequestBody CreateUserRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {
        UserDTO user = userService.createUser(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("User created successfully", user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(
        summary = "Update user",
        description = "Updates user information. Only SUPER_ADMIN and ADMIN can update users in their organization."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User updated successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid update request"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - user not authenticated"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - user not in your organization or insufficient permissions"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found"
        )
    })
    public ResponseEntity<ApiResponseDto<UserDTO>> updateUser(
            @Parameter(description = "User ID to update", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "User update details",
                required = true,
                content = @Content(schema = @Schema(implementation = UpdateUserRequest.class))
            )
            @Valid @RequestBody UpdateUserRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {
        UserDTO user = userService.updateUser(id, request, currentUser);
        return ResponseEntity.ok(ApiResponseDto.success("User updated", user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(
        summary = "Deactivate user",
        description = "Deactivates a user (soft delete). The user will no longer be able to access the system."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User deactivated successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - user not authenticated"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - user not in your organization or insufficient permissions"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found"
        )
    })
    public ResponseEntity<ApiResponseDto<Void>> deleteUser(
            @Parameter(description = "User ID to deactivate", required = true)
            @PathVariable Long id,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {
        userService.deleteUser(id, currentUser);
        return ResponseEntity.ok(ApiResponseDto.success("User deactivated"));
    }
}

