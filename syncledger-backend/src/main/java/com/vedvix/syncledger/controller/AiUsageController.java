package com.vedvix.syncledger.controller;

import com.vedvix.syncledger.dto.AiUsageDTO;
import com.vedvix.syncledger.dto.ApiResponseDto;
import com.vedvix.syncledger.repository.AiUsageLogRepository;
import com.vedvix.syncledger.repository.OrganizationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for AI usage tracking and billing management.
 * Provides per-organization token consumption and cost data for Super Admin.
 *
 * @author vedvix
 */
@Slf4j
@RestController
@RequestMapping("/v1/ai-usage")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "AI Usage", description = "AI token usage tracking and billing management for super administrators")
public class AiUsageController {

    private final AiUsageLogRepository aiUsageLogRepository;
    private final OrganizationRepository organizationRepository;

    /**
     * Get platform-wide usage summary with per-org breakdown.
     */
    @GetMapping("/summary")
    @Operation(summary = "Get platform AI usage summary",
               description = "Returns platform totals and per-organization breakdown of AI token usage and costs")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<ApiResponseDto<AiUsageDTO.PlatformUsageSummary>> getPlatformSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // Get per-org usage
        List<Object[]> orgUsage;
        Object[] platformTotals;

        if (startDate != null && endDate != null) {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(LocalTime.MAX);
            orgUsage = aiUsageLogRepository.getUsageSummaryByOrganization(start, end);
            platformTotals = aiUsageLogRepository.getPlatformUsageSummary(start, end);
        } else {
            orgUsage = aiUsageLogRepository.getUsageSummaryByOrganizationAllTime();
            platformTotals = aiUsageLogRepository.getPlatformUsageSummaryAllTime();
        }

        Object[] normalizedPlatformTotals = normalizePlatformTotals(platformTotals);

        List<AiUsageDTO.OrgUsageSummary> orgSummaries = orgUsage.stream()
                .map(this::mapToOrgSummary)
                .collect(Collectors.toList());

        AiUsageDTO.PlatformUsageSummary summary = AiUsageDTO.PlatformUsageSummary.builder()
                .totalExtractions(toLong(valueAt(normalizedPlatformTotals, 4)))
                .totalInputTokens(toLong(valueAt(normalizedPlatformTotals, 0)))
                .totalOutputTokens(toLong(valueAt(normalizedPlatformTotals, 1)))
                .totalTokens(toLong(valueAt(normalizedPlatformTotals, 2)))
                .totalCostUsd(toBigDecimal(valueAt(normalizedPlatformTotals, 3)))
                .organizationUsage(orgSummaries)
                .build();

        return ResponseEntity.ok(ApiResponseDto.success(summary));
    }

    /**
     * Get detailed usage for a specific organization.
     */
    @GetMapping("/organizations/{orgId}")
    @Operation(summary = "Get organization AI usage detail",
               description = "Returns detailed AI usage, daily breakdown, and tier analysis for a specific organization")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<ApiResponseDto<AiUsageDTO.OrgUsageDetail>> getOrganizationUsage(
            @PathVariable Long orgId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // Default to last 30 days if no dates provided
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : LocalDateTime.now();
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : end.minusDays(30);

        String orgName = organizationRepository.findById(orgId)
                .map(org -> org.getName())
                .orElse("Unknown Organization");

        // Get summary
        List<Object[]> orgUsage;
        if (startDate != null && endDate != null) {
            orgUsage = aiUsageLogRepository.getUsageSummaryByOrganization(start, end);
        } else {
            orgUsage = aiUsageLogRepository.getUsageSummaryByOrganizationAllTime();
        }

        AiUsageDTO.OrgUsageSummary summary = orgUsage.stream()
                .filter(row -> ((Number) row[0]).longValue() == orgId)
                .findFirst()
                .map(this::mapToOrgSummary)
                .orElse(AiUsageDTO.OrgUsageSummary.builder()
                        .organizationId(orgId)
                        .organizationName(orgName)
                        .totalExtractions(0L)
                        .totalInputTokens(0L)
                        .totalOutputTokens(0L)
                        .totalTokens(0L)
                        .totalCostUsd(BigDecimal.ZERO)
                        .build());

        // Get daily breakdown
        List<AiUsageDTO.DailyUsage> dailyUsage = aiUsageLogRepository
                .getDailyUsageForOrganization(orgId, start, end)
                .stream()
                .map(row -> AiUsageDTO.DailyUsage.builder()
                        .date((LocalDate) row[0])
                        .totalTokens(toLong(row[1]))
                        .costUsd(toBigDecimal(row[2]))
                        .extractions(toLong(row[3]))
                        .build())
                .collect(Collectors.toList());

        // Get tier breakdown
        List<AiUsageDTO.TierBreakdown> tierBreakdown = aiUsageLogRepository
                .getUsageByTierForOrganization(orgId, start, end)
                .stream()
                .map(row -> AiUsageDTO.TierBreakdown.builder()
                        .tier((String) row[0])
                        .totalTokens(toLong(row[1]))
                        .costUsd(toBigDecimal(row[2]))
                        .extractions(toLong(row[3]))
                        .build())
                .collect(Collectors.toList());

        AiUsageDTO.OrgUsageDetail detail = AiUsageDTO.OrgUsageDetail.builder()
                .organizationId(orgId)
                .organizationName(orgName)
                .summary(summary)
                .dailyUsage(dailyUsage)
                .tierBreakdown(tierBreakdown)
                .build();

        return ResponseEntity.ok(ApiResponseDto.success(detail));
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private AiUsageDTO.OrgUsageSummary mapToOrgSummary(Object[] row) {
        return AiUsageDTO.OrgUsageSummary.builder()
                .organizationId(toLong(row[0]))
                .organizationName((String) row[1])
                .totalInputTokens(toLong(row[2]))
                .totalOutputTokens(toLong(row[3]))
                .totalTokens(toLong(row[4]))
                .totalCostUsd(toBigDecimal(row[5]))
                .totalExtractions(toLong(row[6]))
                .build();
    }

        private Object[] normalizePlatformTotals(Object[] totals) {
                if (totals == null) {
                        return new Object[]{0L, 0L, 0L, BigDecimal.ZERO, 0L};
                }
                if (totals.length == 1 && totals[0] instanceof Object[]) {
                        return (Object[]) totals[0];
                }
                return totals;
        }

        private Object valueAt(Object[] array, int index) {
                if (array == null || index < 0 || index >= array.length) {
                        return null;
                }
                return array[index];
        }

    private Long toLong(Object val) {
        if (val == null) return 0L;
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.parseLong(val.toString());
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        if (val instanceof Number) return BigDecimal.valueOf(((Number) val).doubleValue());
        return new BigDecimal(val.toString());
    }
}
