package com.vedvix.syncledger.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Analytics data for a specific vendor.
 * 
 * @author vedvix
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorAnalyticsDTO {
    // Invoice counts
    private Long totalInvoices;
    private Long pendingInvoices;
    private Long approvedInvoices;
    private Long rejectedInvoices;
    private Long syncedInvoices;
    
    // Financial metrics
    private BigDecimal totalAmount;
    private BigDecimal averageInvoiceAmount;
    private BigDecimal minInvoiceAmount;
    private BigDecimal maxInvoiceAmount;
    private BigDecimal totalTaxAmount;
    
    // Processing metrics
    private BigDecimal averageConfidenceScore;
    private Long invoicesRequiringReview;
    
    // Timeline
    private LocalDate firstInvoiceDate;
    private LocalDate lastInvoiceDate;
    
    // Monthly trend (month -> amount)
    private Map<String, BigDecimal> monthlyTotals;
}
