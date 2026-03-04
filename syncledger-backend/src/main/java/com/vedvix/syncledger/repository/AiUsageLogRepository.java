package com.vedvix.syncledger.repository;

import com.vedvix.syncledger.model.AiUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for AI usage tracking and per-organization metering queries.
 *
 * @author vedvix
 */
@Repository
public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, Long> {

    // ==================== Per-Organization Queries ====================

    /**
     * Get total tokens and cost for an organization in a date range.
     * Returns [orgId, orgName, totalInputTokens, totalOutputTokens, totalTokens, totalCostUsd, extractionCount]
     */
    @Query("SELECT a.organization.id, a.organization.name, " +
           "COALESCE(SUM(a.inputTokens), 0), COALESCE(SUM(a.outputTokens), 0), " +
           "COALESCE(SUM(a.totalTokens), 0), COALESCE(SUM(a.estimatedCostUsd), 0), COUNT(a) " +
           "FROM AiUsageLog a " +
           "WHERE a.createdAt BETWEEN :start AND :end " +
           "GROUP BY a.organization.id, a.organization.name " +
           "ORDER BY SUM(a.totalTokens) DESC")
    List<Object[]> getUsageSummaryByOrganization(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Get all-time usage summary per organization.
     */
    @Query("SELECT a.organization.id, a.organization.name, " +
           "COALESCE(SUM(a.inputTokens), 0), COALESCE(SUM(a.outputTokens), 0), " +
           "COALESCE(SUM(a.totalTokens), 0), COALESCE(SUM(a.estimatedCostUsd), 0), COUNT(a) " +
           "FROM AiUsageLog a " +
           "GROUP BY a.organization.id, a.organization.name " +
           "ORDER BY SUM(a.totalTokens) DESC")
    List<Object[]> getUsageSummaryByOrganizationAllTime();

    /**
     * Get daily usage for a specific organization (for charts).
     * Returns [date, totalTokens, costUsd, extractionCount]
     */
    @Query("SELECT CAST(a.createdAt AS LocalDate), " +
           "COALESCE(SUM(a.totalTokens), 0), COALESCE(SUM(a.estimatedCostUsd), 0), COUNT(a) " +
           "FROM AiUsageLog a " +
           "WHERE a.organization.id = :orgId AND a.createdAt BETWEEN :start AND :end " +
           "GROUP BY CAST(a.createdAt AS LocalDate) " +
           "ORDER BY CAST(a.createdAt AS LocalDate)")
    List<Object[]> getDailyUsageForOrganization(
            @Param("orgId") Long orgId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Get usage breakdown by AI tier for an organization.
     * Returns [aiTier, totalTokens, costUsd, count]
     */
    @Query("SELECT a.aiTier, COALESCE(SUM(a.totalTokens), 0), " +
           "COALESCE(SUM(a.estimatedCostUsd), 0), COUNT(a) " +
           "FROM AiUsageLog a " +
           "WHERE a.organization.id = :orgId AND a.createdAt BETWEEN :start AND :end " +
           "GROUP BY a.aiTier")
    List<Object[]> getUsageByTierForOrganization(
            @Param("orgId") Long orgId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Get platform-wide totals for a date range.
     * Returns [totalInputTokens, totalOutputTokens, totalTokens, totalCostUsd, extractionCount]
     */
    @Query("SELECT COALESCE(SUM(a.inputTokens), 0), COALESCE(SUM(a.outputTokens), 0), " +
           "COALESCE(SUM(a.totalTokens), 0), COALESCE(SUM(a.estimatedCostUsd), 0), COUNT(a) " +
           "FROM AiUsageLog a " +
           "WHERE a.createdAt BETWEEN :start AND :end")
    Object[] getPlatformUsageSummary(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Get platform-wide totals (all time).
     */
    @Query("SELECT COALESCE(SUM(a.inputTokens), 0), COALESCE(SUM(a.outputTokens), 0), " +
           "COALESCE(SUM(a.totalTokens), 0), COALESCE(SUM(a.estimatedCostUsd), 0), COUNT(a) " +
           "FROM AiUsageLog a")
    Object[] getPlatformUsageSummaryAllTime();

    /**
     * Count extractions for org in current month (for quota checking).
     */
    @Query("SELECT COUNT(a) FROM AiUsageLog a " +
           "WHERE a.organization.id = :orgId AND a.createdAt BETWEEN :start AND :end")
    Long countExtractionsForOrganization(
            @Param("orgId") Long orgId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
