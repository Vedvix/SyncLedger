package com.vedvix.syncledger.controller;

import com.vedvix.syncledger.dto.*;
import com.vedvix.syncledger.model.InvoiceStatus;
import com.vedvix.syncledger.security.UserPrincipal;
import com.vedvix.syncledger.service.ExcelExportService;
import com.vedvix.syncledger.service.InvoiceService;
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
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.web.multipart.MultipartFile;

/**
 * Invoice Controller with multi-tenant support.
 * Organization-scoped access is enforced at service layer.
 * 
 * @author vedvix
 */
@RestController
@RequestMapping("/v1/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "API endpoints for invoice management, processing, and approval workflows")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final ExcelExportService excelExportService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'APPROVER', 'VIEWER')")
    @Operation(
        summary = "Get invoices",
        description = "Retrieves a paginated list of invoices from the authenticated user's organization. Supports filtering by search term and status. All users can view invoices."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Invoices retrieved successfully",
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
    public ResponseEntity<ApiResponseDto<PagedResponse<InvoiceDTO>>> getInvoices(
            @Parameter(description = "Pagination information (page, size, sort)", required = false)
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @Parameter(description = "Search query to filter invoices by invoice number, vendor, or amount")
            @RequestParam(required = false) String search,
            @Parameter(description = "Filter by invoice status (comma-separated, e.g. PENDING,UNDER_REVIEW)")
            @RequestParam(required = false) String status,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        List<InvoiceStatus> statuses = null;
        if (status != null && !status.isBlank()) {
            statuses = Arrays.stream(status.split(","))
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .map(InvoiceStatus::valueOf)
                    .collect(Collectors.toList());
        }
        PagedResponse<InvoiceDTO> invoices = invoiceService.getInvoices(pageable, search, statuses, currentUser);
        return ResponseEntity.ok(ApiResponseDto.success(invoices));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'APPROVER', 'VIEWER')")
    @Operation(
        summary = "Get invoice by ID",
        description = "Retrieves detailed information about a specific invoice including all line items and approval history"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Invoice retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - user not authenticated"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - invoice not in your organization"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Invoice not found"
        )
    })
    public ResponseEntity<ApiResponseDto<InvoiceDTO>> getInvoice(
            @Parameter(description = "Invoice ID", required = true)
            @PathVariable Long id,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {
        InvoiceDTO invoice = invoiceService.getInvoiceById(id, currentUser);
        return ResponseEntity.ok(ApiResponseDto.success(invoice));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(
        summary = "Update invoice",
        description = "Updates invoice details such as vendor information, amounts, and line items. Only SUPER_ADMIN and ADMIN can update invoices."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Invoice updated successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid update request or invoice cannot be updated in current status"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - user not authenticated"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - insufficient permissions or invoice not in your organization"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Invoice not found"
        )
    })
    public ResponseEntity<ApiResponseDto<InvoiceDTO>> updateInvoice(
            @Parameter(description = "Invoice ID to update", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Invoice update details",
                required = true,
                content = @Content(schema = @Schema(implementation = UpdateInvoiceRequest.class))
            )
            @Valid @RequestBody UpdateInvoiceRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {
        InvoiceDTO invoice = invoiceService.updateInvoice(id, request, currentUser);
        return ResponseEntity.ok(ApiResponseDto.success("Invoice updated", invoice));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'APPROVER')")
    @Operation(
        summary = "Approve invoice",
        description = "Approves an invoice and moves it to APPROVED status. Only SUPER_ADMIN, ADMIN, and APPROVER roles can approve invoices."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Invoice approved successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invoice cannot be approved in current status"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - user not authenticated"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - insufficient permissions"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Invoice not found"
        )
    })
    public ResponseEntity<ApiResponseDto<InvoiceDTO>> approveInvoice(
            @Parameter(description = "Invoice ID to approve", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Optional approval comments (notes)",
                required = false,
                content = @Content(schema = @Schema(implementation = ApprovalRequest.class))
            )
            @RequestBody(required = false) ApprovalRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {
        String notes = request != null ? request.getComments() : null;
        InvoiceDTO invoice = invoiceService.approveInvoice(id, notes, currentUser);
        return ResponseEntity.ok(ApiResponseDto.success("Invoice approved", invoice));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'APPROVER')")
    @Operation(
        summary = "Reject invoice",
        description = "Rejects an invoice with a required rejection reason. Invoice goes back to DRAFT status for corrections."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Invoice rejected successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invoice cannot be rejected in current status or rejection reason is missing"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - user not authenticated"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - insufficient permissions"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Invoice not found"
        )
    })
    public ResponseEntity<ApiResponseDto<InvoiceDTO>> rejectInvoice(
            @Parameter(description = "Invoice ID to reject", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Rejection details with required reason",
                required = true,
                content = @Content(schema = @Schema(implementation = ApprovalRequest.class))
            )
            @Valid @RequestBody ApprovalRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal currentUser) {
        InvoiceDTO invoice = invoiceService.rejectInvoice(id, request.getRejectionReason(), currentUser);
        return ResponseEntity.ok(ApiResponseDto.success("Invoice rejected", invoice));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'APPROVER', 'VIEWER')")
    @Operation(
        summary = "Download invoice PDF",
        description = "Downloads the original invoice PDF file"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
        @ApiResponse(responseCode = "404", description = "Invoice or file not found")
    })
    public ResponseEntity<InputStreamResource> downloadInvoice(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        InputStream fileStream = invoiceService.downloadInvoiceFile(id, currentUser);
        String fileName = invoiceService.getInvoiceFileName(id, currentUser);
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + (fileName != null ? fileName : "invoice.pdf") + "\"")
                .body(new InputStreamResource(fileStream));
    }

    @GetMapping("/{id}/preview")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'APPROVER', 'VIEWER')")
    @Operation(
        summary = "Preview invoice PDF",
        description = "Serves the invoice PDF inline for browser preview (iframe/embed)"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File served for preview"),
        @ApiResponse(responseCode = "404", description = "Invoice or file not found")
    })
    public ResponseEntity<InputStreamResource> previewInvoice(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        InputStream fileStream = invoiceService.downloadInvoiceFile(id, currentUser);
        String fileName = invoiceService.getInvoiceFileName(id, currentUser);
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "inline; filename=\"" + (fileName != null ? fileName : "invoice.pdf") + "\"")
                .body(new InputStreamResource(fileStream));
    }

    // ── Upload Invoice PDF ───────────────────────────────────────────────
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(
        summary = "Upload invoice PDF",
        description = "Upload a PDF file, store it (S3 or local), create an invoice record, and trigger extraction through the PDF microservice"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Invoice uploaded and processing started"),
        @ApiResponse(responseCode = "400", description = "Invalid file type or empty file")
    })
    public ResponseEntity<ApiResponseDto<InvoiceDTO>> uploadInvoice(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        if (file.isEmpty() || !"application/pdf".equals(file.getContentType())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("Only non-empty PDF files are accepted"));
        }
        InvoiceDTO invoice = invoiceService.uploadInvoice(file, currentUser);
        return ResponseEntity.ok(ApiResponseDto.success("Invoice uploaded and processing started", invoice));
    }

    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'APPROVER', 'VIEWER')")
    @Operation(
        summary = "Get dashboard statistics",
        description = "Retrieves invoice statistics for the dashboard including counts by status, total amounts, and approval metrics"
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
            @AuthenticationPrincipal UserPrincipal currentUser) {
        DashboardStatsDTO stats = invoiceService.getDashboardStats(currentUser);
        return ResponseEntity.ok(ApiResponseDto.success(stats));
    }

    // ==================== Export Endpoints ====================

    @PostMapping("/export")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'APPROVER', 'VIEWER')")
    @Operation(
        summary = "Export invoices to Excel",
        description = "Exports filtered invoices to an Excel (.xlsx) file. Supports advanced filtering, column selection, optional line items and summary sheets. Accountants can create custom filter views and download the data for analysis."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Excel file generated successfully",
            content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - user not authenticated"
        )
    })
    public ResponseEntity<byte[]> exportInvoices(
            @RequestBody(required = false) ExportRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) throws Exception {
        
        if (request == null) {
            request = new ExportRequest();
        }

        byte[] excelData = excelExportService.exportToExcel(request, currentUser);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "SyncLedger_Invoices_" + timestamp + ".xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        headers.setContentLength(excelData.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }

    @GetMapping("/export/columns")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'APPROVER', 'VIEWER')")
    @Operation(
        summary = "Get available export columns",
        description = "Returns a map of available column keys and their display labels for use in the export column selector."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<ApiResponseDto<Map<String, String>>> getExportColumns() {
        return ResponseEntity.ok(ApiResponseDto.success(excelExportService.getAvailableColumns()));
    }
}

