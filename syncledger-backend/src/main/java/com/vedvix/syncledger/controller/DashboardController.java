package com.vedvix.syncledger.controller;

import com.vedvix.syncledger.dto.ApiResponseDto;
import com.vedvix.syncledger.dto.DashboardStatsDTO;
import com.vedvix.syncledger.security.UserPrincipal;
import com.vedvix.syncledger.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Dashboard Controller for retrieving dashboard statistics.
 * 
 * @author vedvix
 */
@Slf4j
@RestController
@RequestMapping("/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard statistics and activity endpoints")
public class DashboardController {

    private final InvoiceService invoiceService;

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'APPROVER', 'VIEWER')")
    @Operation(
        summary = "Get dashboard statistics",
        description = "Retrieves invoice statistics for the dashboard including counts by status, total amounts, and approval metrics. Optionally filter by date range."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Dashboard statistics retrieved successfully"
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
    public ResponseEntity<ApiResponseDto<DashboardStatsDTO>> getDashboardStats(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "Start date filter (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date filter (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.debug("Getting dashboard stats for user: {}, dateRange: {} - {}", currentUser.getEmail(), startDate, endDate);
        DashboardStatsDTO stats = invoiceService.getDashboardStats(currentUser, startDate, endDate);
        return ResponseEntity.ok(ApiResponseDto.success(stats));
    }

    @GetMapping("/activity")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'APPROVER', 'VIEWER')")
    @Operation(
        summary = "Get recent activity",
        description = "Retrieves recent activity for the dashboard"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<ApiResponseDto<Object>> getRecentActivity(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {
        log.debug("Getting recent activity for user: {}", currentUser.getEmail());
        // Return empty array for now - can be implemented later
        return ResponseEntity.ok(ApiResponseDto.success(new Object[0]));
    }
}
