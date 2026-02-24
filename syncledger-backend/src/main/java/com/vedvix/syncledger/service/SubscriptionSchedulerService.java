package com.vedvix.syncledger.service;

import com.vedvix.syncledger.model.Subscription;
import com.vedvix.syncledger.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled service that monitors subscription and trial expiry.
 * Runs daily to:
 * 1. Send reminder emails for expiring trials (3d, 1d before)
 * 2. Expire and suspend overdue trials
 * 3. Send reminder emails for expiring subscriptions (7d, 3d before)
 * 4. Expire and suspend overdue subscriptions
 * 
 * @author vedvix
 */
@Slf4j
@Service
@EnableScheduling
@RequiredArgsConstructor
public class SubscriptionSchedulerService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;
    private final SubscriptionEmailService emailService;

    /**
     * Daily check for trial and subscription lifecycle events.
     * Runs every day at 6:00 AM UTC.
     */
    @Scheduled(cron = "${subscription.scheduler.cron:0 0 6 * * *}")
    public void processSubscriptionLifecycle() {
        log.info("Starting subscription lifecycle check...");

        long startTime = System.currentTimeMillis();
        int processed = 0;

        processed += processTrialExpiring3Days();
        processed += processTrialExpiring1Day();
        processed += processExpiredTrials();
        processed += processSubscriptionExpiring7Days();
        processed += processSubscriptionExpiring3Days();
        processed += processExpiredSubscriptions();

        long duration = System.currentTimeMillis() - startTime;
        log.info("Subscription lifecycle check completed in {}ms, processed {} events", duration, processed);
    }

    /**
     * Send 3-day trial expiry warnings.
     */
    @Transactional
    int processTrialExpiring3Days() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeDays = now.plusDays(3);

        List<Subscription> expiring = subscriptionRepository
                .findTrialsExpiring3DaysNotNotified(now, threeDays);

        for (Subscription sub : expiring) {
            try {
                emailService.sendTrialExpiring3DaysEmail(sub.getOrganization(), sub);
                sub.setTrialExpiryNotified3d(true);
                subscriptionRepository.save(sub);
                log.info("3-day trial expiry notification sent for org: {}",
                        sub.getOrganization().getName());
            } catch (Exception e) {
                log.error("Failed to process 3-day trial notification for org {}: {}",
                        sub.getOrganization().getId(), e.getMessage());
            }
        }

        return expiring.size();
    }

    /**
     * Send 1-day trial expiry warnings.
     */
    @Transactional
    int processTrialExpiring1Day() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneDay = now.plusDays(1);

        List<Subscription> expiring = subscriptionRepository
                .findTrialsExpiring1DayNotNotified(now, oneDay);

        for (Subscription sub : expiring) {
            try {
                emailService.sendTrialExpiring1DayEmail(sub.getOrganization(), sub);
                sub.setTrialExpiryNotified1d(true);
                subscriptionRepository.save(sub);
                log.info("1-day trial expiry notification sent for org: {}",
                        sub.getOrganization().getName());
            } catch (Exception e) {
                log.error("Failed to process 1-day trial notification for org {}: {}",
                        sub.getOrganization().getId(), e.getMessage());
            }
        }

        return expiring.size();
    }

    /**
     * Process expired trials - suspend access and notify.
     */
    @Transactional
    int processExpiredTrials() {
        LocalDateTime now = LocalDateTime.now();
        List<Subscription> expired = subscriptionRepository.findExpiredTrialsNotNotified(now);

        for (Subscription sub : expired) {
            try {
                subscriptionService.expireTrialSubscription(sub);
                emailService.sendTrialExpiredEmail(sub.getOrganization());
                sub.setTrialExpiredNotified(true);
                subscriptionRepository.save(sub);
                log.info("Trial expired and org suspended: {}", sub.getOrganization().getName());
            } catch (Exception e) {
                log.error("Failed to process expired trial for org {}: {}",
                        sub.getOrganization().getId(), e.getMessage());
            }
        }

        return expired.size();
    }

    /**
     * Send 7-day subscription expiry warnings.
     */
    @Transactional
    int processSubscriptionExpiring7Days() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDays = now.plusDays(7);

        List<Subscription> expiring = subscriptionRepository
                .findSubscriptionsExpiring7DaysNotNotified(now, sevenDays);

        for (Subscription sub : expiring) {
            try {
                emailService.sendSubscriptionExpiring7DaysEmail(sub.getOrganization());
                sub.setSubscriptionExpiryNotified7d(true);
                subscriptionRepository.save(sub);
                log.info("7-day subscription expiry notification sent for org: {}",
                        sub.getOrganization().getName());
            } catch (Exception e) {
                log.error("Failed to process 7-day subscription notification for org {}: {}",
                        sub.getOrganization().getId(), e.getMessage());
            }
        }

        return expiring.size();
    }

    /**
     * Send 3-day subscription expiry warnings.
     */
    @Transactional
    int processSubscriptionExpiring3Days() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeDays = now.plusDays(3);

        List<Subscription> expiring = subscriptionRepository
                .findSubscriptionsExpiring3DaysNotNotified(now, threeDays);

        for (Subscription sub : expiring) {
            try {
                emailService.sendSubscriptionExpiring3DaysEmail(sub.getOrganization());
                sub.setSubscriptionExpiryNotified3d(true);
                subscriptionRepository.save(sub);
                log.info("3-day subscription expiry notification sent for org: {}",
                        sub.getOrganization().getName());
            } catch (Exception e) {
                log.error("Failed to process 3-day subscription notification for org {}: {}",
                        sub.getOrganization().getId(), e.getMessage());
            }
        }

        return expiring.size();
    }

    /**
     * Process expired subscriptions - suspend access and notify.
     */
    @Transactional
    int processExpiredSubscriptions() {
        LocalDateTime now = LocalDateTime.now();
        List<Subscription> expired = subscriptionRepository.findExpiredSubscriptionsNotNotified(now);

        for (Subscription sub : expired) {
            try {
                subscriptionService.expireActiveSubscription(sub);
                emailService.sendSubscriptionExpiredEmail(sub.getOrganization());
                sub.setSubscriptionExpiredNotified(true);
                subscriptionRepository.save(sub);
                log.info("Subscription expired and org suspended: {}", sub.getOrganization().getName());
            } catch (Exception e) {
                log.error("Failed to process expired subscription for org {}: {}",
                        sub.getOrganization().getId(), e.getMessage());
            }
        }

        return expired.size();
    }
}
