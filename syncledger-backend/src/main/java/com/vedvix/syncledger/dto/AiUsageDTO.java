package com.vedvix.syncledger.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Data Transfer Objects for AI usage tracking and billing.
 * Used by Super Admin portal to monitor per-organization AI token consumption.
 *
 * @author vedvix
 */
public class AiUsageDTO {

    /**
     * Per-organization usage summary.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrgUsageSummary {
        private Long organizationId;
        private String organizationName;
        private Long totalExtractions;
        private Long totalInputTokens;
        private Long totalOutputTokens;
        private Long totalTokens;
        private BigDecimal totalCostUsd;
    }

    /**
     * Daily usage breakdown (for charts).
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyUsage {
        private LocalDate date;
        private Long extractions;
        private Long totalTokens;
        private BigDecimal costUsd;
    }

    /**
     * Usage breakdown by AI tier.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TierBreakdown {
        private String tier;
        private Long extractions;
        private Long totalTokens;
        private BigDecimal costUsd;
    }

    /**
     * Detailed organization usage response with daily breakdown.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrgUsageDetail {
        private Long organizationId;
        private String organizationName;
        private OrgUsageSummary summary;
        private List<DailyUsage> dailyUsage;
        private List<TierBreakdown> tierBreakdown;
    }

    /**
     * Platform-wide usage summary.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PlatformUsageSummary {
        private Long totalExtractions;
        private Long totalInputTokens;
        private Long totalOutputTokens;
        private Long totalTokens;
        private BigDecimal totalCostUsd;
        private List<OrgUsageSummary> organizationUsage;
    }
}
