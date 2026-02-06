package com.vedvix.syncledger.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object for dashboard statistics.
 * 
 * @author vedvix
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDTO {

    // Invoice Counts
    private Long totalInvoices;
    private Long pendingInvoices;
    private Long underReviewInvoices;
    private Long approvedInvoices;
    private Long rejectedInvoices;
    private Long syncedInvoices;
    private Long overdueInvoices;

    // Financial Summary
    private BigDecimal totalAmount;
    private BigDecimal pendingAmount;
    private BigDecimal approvedAmount;
    private BigDecimal syncedAmount;

    // Processing Metrics
    private Long invoicesProcessedToday;
    private Long invoicesProcessedThisWeek;
    private Long invoicesProcessedThisMonth;
    private Double averageProcessingTimeMs;

    // Email Metrics
    private Long emailsProcessedToday;
    private Long unprocessedEmails;
    private Long emailsWithErrors;

    // Sync Metrics
    private Long pendingSyncs;
    private Long failedSyncs;
    private Long successfulSyncsToday;
    private Double syncSuccessRate;

    // User Activity
    private Long activeUsers;
    private Long totalUsers;

    // Charts Data
    private List<MonthlyStats> monthlyTrends;
    private List<VendorStats> topVendors;
    private Map<String, Long> invoicesByStatus;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonthlyStats {
        private Integer year;
        private Integer month;
        private String monthName;
        private Long invoiceCount;
        private BigDecimal totalAmount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VendorStats {
        private String vendorName;
        private Long invoiceCount;
        private BigDecimal totalAmount;
    }
}
