package com.vedvix.syncledger.repository;

import com.vedvix.syncledger.model.Subscription;
import com.vedvix.syncledger.model.SubscriptionPlan;
import com.vedvix.syncledger.model.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Subscription entity.
 * 
 * @author vedvix
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    /**
     * Find subscription by organization ID.
     */
    Optional<Subscription> findByOrganization_Id(Long organizationId);

    /**
     * Find all subscriptions by status.
     */
    List<Subscription> findByStatus(SubscriptionStatus status);

    /**
     * Find trials expiring within a date range.
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = 'TRIAL' " +
           "AND s.trialEndDate BETWEEN :start AND :end")
    List<Subscription> findTrialsExpiringBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Find expired trials not yet notified.
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = 'TRIAL' " +
           "AND s.trialEndDate < :now AND s.trialExpiredNotified = false")
    List<Subscription> findExpiredTrialsNotNotified(@Param("now") LocalDateTime now);

    /**
     * Find trials expiring in 3 days not yet notified.
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = 'TRIAL' " +
           "AND s.trialEndDate BETWEEN :now AND :threeDays " +
           "AND s.trialExpiryNotified3d = false")
    List<Subscription> findTrialsExpiring3DaysNotNotified(
            @Param("now") LocalDateTime now,
            @Param("threeDays") LocalDateTime threeDays);

    /**
     * Find trials expiring in 1 day not yet notified.
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = 'TRIAL' " +
           "AND s.trialEndDate BETWEEN :now AND :oneDay " +
           "AND s.trialExpiryNotified1d = false")
    List<Subscription> findTrialsExpiring1DayNotNotified(
            @Param("now") LocalDateTime now,
            @Param("oneDay") LocalDateTime oneDay);

    /**
     * Find subscriptions expiring in 7 days not yet notified.
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' " +
           "AND s.subscriptionEndDate BETWEEN :now AND :sevenDays " +
           "AND s.subscriptionExpiryNotified7d = false")
    List<Subscription> findSubscriptionsExpiring7DaysNotNotified(
            @Param("now") LocalDateTime now,
            @Param("sevenDays") LocalDateTime sevenDays);

    /**
     * Find subscriptions expiring in 3 days not yet notified.
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' " +
           "AND s.subscriptionEndDate BETWEEN :now AND :threeDays " +
           "AND s.subscriptionExpiryNotified3d = false")
    List<Subscription> findSubscriptionsExpiring3DaysNotNotified(
            @Param("now") LocalDateTime now,
            @Param("threeDays") LocalDateTime threeDays);

    /**
     * Find expired subscriptions not yet notified.
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' " +
           "AND s.subscriptionEndDate < :now AND s.subscriptionExpiredNotified = false")
    List<Subscription> findExpiredSubscriptionsNotNotified(@Param("now") LocalDateTime now);

    /**
     * Find subscription by Stripe subscription ID.
     */
    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);

    /**
     * Find subscription by Stripe customer ID.
     */
    Optional<Subscription> findByStripeCustomerId(String stripeCustomerId);

    /**
     * Count subscriptions by plan.
     */
    long countByPlan(SubscriptionPlan plan);

    /**
     * Count subscriptions by status.
     */
    long countByStatus(SubscriptionStatus status);
}
