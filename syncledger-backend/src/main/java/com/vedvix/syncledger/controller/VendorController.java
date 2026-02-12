package com.vedvix.syncledger.controller;

import com.vedvix.syncledger.dto.*;
import com.vedvix.syncledger.security.UserPrincipal;
import com.vedvix.syncledger.service.VendorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * REST Controller for Vendor management and analytics.
 * 
 * @author vedvix
 */
@RestController
@RequestMapping("/v1/vendors")
@RequiredArgsConstructor
@Tag(name = "Vendors", description = "Vendor management and analytics endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class VendorController {

    private final VendorService vendorService;

    // ─── CRUD Endpoints ─────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "List vendors", description = "Get paginated list of vendors for current organization")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Vendors retrieved successfully")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'APPROVER', 'VIEWER')")
    public ResponseEntity<ApiResponseDto<PagedResponse<VendorDTO>>> getVendors(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        PagedResponse<VendorDTO> vendors = vendorService.getVendors(pageable, search, currentUser);
        return ResponseEntity.ok(ApiResponseDto.success("Vendors retrieved successfully", vendors));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get vendor by ID", description = "Get vendor details with analytics")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Vendor retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Vendor not found")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'APPROVER', 'VIEWER')")
    public ResponseEntity<ApiResponseDto<VendorDTO>> getVendor(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        VendorDTO vendor = vendorService.getVendorById(id, currentUser);
        return ResponseEntity.ok(ApiResponseDto.success("Vendor retrieved successfully", vendor));
    }

    @PostMapping
    @Operation(summary = "Create vendor", description = "Create a new vendor in the current organization")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Vendor created successfully"),
        @ApiResponse(responseCode = "400", description = "Vendor already exists or invalid data")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponseDto<VendorDTO>> createVendor(
            @RequestBody VendorRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        VendorDTO vendor = vendorService.createVendor(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Vendor created successfully", vendor));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update vendor", description = "Update an existing vendor")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Vendor updated successfully"),
        @ApiResponse(responseCode = "404", description = "Vendor not found")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponseDto<VendorDTO>> updateVendor(
            @PathVariable Long id,
            @RequestBody VendorRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        VendorDTO vendor = vendorService.updateVendor(id, request, currentUser);
        return ResponseEntity.ok(ApiResponseDto.success("Vendor updated successfully", vendor));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete vendor", description = "Delete a vendor (only if no linked invoices)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Vendor deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Cannot delete vendor with linked invoices"),
        @ApiResponse(responseCode = "404", description = "Vendor not found")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponseDto<Void>> deleteVendor(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        vendorService.deleteVendor(id, currentUser);
        return ResponseEntity.ok(ApiResponseDto.success("Vendor deleted successfully"));
    }

    // ─── Analytics Endpoints ────────────────────────────────────────────

    @GetMapping("/{id}/analytics")
    @Operation(summary = "Get vendor analytics", description = "Get detailed analytics for a specific vendor")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Analytics retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Vendor not found")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'APPROVER', 'VIEWER')")
    public ResponseEntity<ApiResponseDto<VendorAnalyticsDTO>> getVendorAnalytics(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        // Access check via service
        vendorService.getVendorById(id, currentUser);
        VendorAnalyticsDTO analytics = vendorService.getVendorAnalytics(id);
        return ResponseEntity.ok(ApiResponseDto.success("Vendor analytics retrieved successfully", analytics));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get vendor summary", description = "Get organization-wide vendor summary with top vendors")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Summary retrieved successfully")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'APPROVER', 'VIEWER')")
    public ResponseEntity<ApiResponseDto<VendorSummaryDTO>> getVendorSummary(
            @AuthenticationPrincipal UserPrincipal currentUser) {

        VendorSummaryDTO summary = vendorService.getVendorSummary(currentUser);
        return ResponseEntity.ok(ApiResponseDto.success("Vendor summary retrieved successfully", summary));
    }
}
