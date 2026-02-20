package com.vedvix.syncledger.dto;

import com.vedvix.syncledger.model.InvoiceStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for exporting invoices with advanced filters.
 * Supports all the filtering options accountants need for analysis.
 * 
 * @author vedvix
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportRequest {

    // ==================== Basic Filters ====================
    
    /** Free-text search across invoice number, vendor, PO number */
    private String search;
    
    /** Filter by invoice statuses */
    private List<String> statuses;
    
    // ==================== Date Filters ====================
    
    /** Invoice date range - from */
    private LocalDate invoiceDateFrom;
    
    /** Invoice date range - to */
    private LocalDate invoiceDateTo;
    
    /** Due date range - from */
    private LocalDate dueDateFrom;
    
    /** Due date range - to */
    private LocalDate dueDateTo;
    
    /** Created/imported date range - from */
    private LocalDate createdDateFrom;
    
    /** Created/imported date range - to */
    private LocalDate createdDateTo;
    
    // ==================== Financial Filters ====================
    
    /** Minimum total amount */
    private BigDecimal minAmount;
    
    /** Maximum total amount */
    private BigDecimal maxAmount;
    
    /** Currency filter (e.g., "USD", "EUR") */
    private String currency;
    
    // ==================== Vendor Filters ====================
    
    /** Filter by specific vendor names */
    private List<String> vendorNames;
    
    /** Filter by vendor ID */
    private Long vendorId;
    
    // ==================== Accounting Filters ====================
    
    /** Filter by GL account */
    private String glAccount;
    
    /** Filter by cost center */
    private String costCenter;
    
    /** Filter by project */
    private String project;
    
    /** Filter by item category */
    private String itemCategory;
    
    // ==================== Processing Filters ====================
    
    /** Show only overdue invoices */
    private Boolean overdueOnly;
    
    /** Show only invoices requiring manual review */
    private Boolean requiresManualReview;
    
    /** Minimum confidence score */
    private BigDecimal minConfidenceScore;
    
    /** Maximum confidence score */
    private BigDecimal maxConfidenceScore;
    
    // ==================== Export Options ====================
    
    /** Which columns to include in export. null = all columns */
    private List<String> columns;
    
    /** Whether to include line items as separate rows */
    private Boolean includeLineItems;
    
    /** Whether to include a summary sheet */
    private Boolean includeSummary;
    
    /** Sort field */
    private String sortBy;
    
    /** Sort direction */
    private String sortDirection;
}
