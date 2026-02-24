package com.vedvix.syncledger.service;

import com.vedvix.syncledger.model.Organization;
import com.vedvix.syncledger.model.Subscription;
import com.vedvix.syncledger.notification.domain.NotificationRequest;
import com.vedvix.syncledger.notification.domain.Recipient;
import com.vedvix.syncledger.notification.enums.NotificationChannel;
import com.vedvix.syncledger.notification.enums.NotificationPriority;
import com.vedvix.syncledger.notification.service.DirectNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Email notification service for subscription lifecycle events.
 * Delegates to the notification module (DirectNotificationService) for
 * template rendering, queuing via RabbitMQ, SES delivery, rate limiting,
 * retry handling, and audit trail.
 *
 * All email sends are async to prevent blocking the main request thread.
 *
 * @author vedvix
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionEmailService {

    private final DirectNotificationService notificationService;

    @Value("${app.base-url:http://localhost:5173}")
    private String appBaseUrl;

    @Value("${app.name:SyncLedger}")
    private String appName;

    // ==================== Trial Notifications ====================

    /**
     * Send welcome email when organization signs up for trial.
     */
    @Async
    public void sendTrialWelcomeEmail(Organization org, String adminEmail, String adminName) {
        sendNotification(
                "subscription_trial_welcome",
                adminEmail,
                org,
                NotificationPriority.HIGH,
                Map.of(
                        "adminName", adminName,
                        "orgName", org.getName(),
                        "trialDays", 15,
                        "appName", appName,
                        "baseUrl", appBaseUrl
                )
        );
        log.info("Trial welcome email queued for {}", adminEmail);
    }

    /**
     * Send trial expiring reminder (3 days before).
     */
    @Async
    public void sendTrialExpiring3DaysEmail(Organization org, Subscription sub) {
        String email = resolveOrgEmail(org);
        sendNotification(
                "subscription_trial_expiring",
                email,
                org,
                NotificationPriority.HIGH,
                Map.of(
                        "orgName", org.getName(),
                        "daysLeft", 3,
                        "daysSuffix", "s",
                        "urgencyColor", "#ff9800",
                        "appName", appName,
                        "baseUrl", appBaseUrl
                )
        );
        log.info("Trial 3-day expiry reminder queued for org: {}", org.getName());
    }

    /**
     * Send trial expiring reminder (1 day before).
     */
    @Async
    public void sendTrialExpiring1DayEmail(Organization org, Subscription sub) {
        String email = resolveOrgEmail(org);
        sendNotification(
                "subscription_trial_expiring",
                email,
                org,
                NotificationPriority.HIGH,
                Map.of(
                        "orgName", org.getName(),
                        "daysLeft", 1,
                        "daysSuffix", "",
                        "urgencyColor", "#dc3545",
                        "appName", appName,
                        "baseUrl", appBaseUrl
                )
        );
        log.info("Trial 1-day expiry reminder queued for org: {}", org.getName());
    }

    /**
     * Send trial expired notification.
     */
    @Async
    public void sendTrialExpiredEmail(Organization org) {
        String email = resolveOrgEmail(org);
        sendNotification(
                "subscription_trial_expired",
                email,
                org,
                NotificationPriority.HIGH,
                Map.of(
                        "orgName", org.getName(),
                        "appName", appName,
                        "baseUrl", appBaseUrl
                )
        );
        log.info("Trial expired email queued for org: {}", org.getName());
    }

    // ==================== Subscription Notifications ====================

    /**
     * Send subscription activated confirmation.
     */
    @Async
    public void sendSubscriptionActivatedEmail(Organization org, Subscription sub) {
        String email = resolveOrgEmail(org);
        sendNotification(
                "subscription_activated",
                email,
                org,
                NotificationPriority.HIGH,
                Map.of(
                        "orgName", org.getName(),
                        "planName", sub.getPlan().getDisplayName(),
                        "expiresAt", sub.getSubscriptionEndDate().toString(),
                        "appName", appName,
                        "baseUrl", appBaseUrl
                )
        );
        log.info("Subscription activated email queued for org: {}", org.getName());
    }

    /**
     * Send subscription expiring (7 days).
     */
    @Async
    public void sendSubscriptionExpiring7DaysEmail(Organization org) {
        String email = resolveOrgEmail(org);
        sendNotification(
                "subscription_expiring",
                email,
                org,
                NotificationPriority.MEDIUM,
                Map.of(
                        "orgName", org.getName(),
                        "daysLeft", 7,
                        "appName", appName,
                        "baseUrl", appBaseUrl
                )
        );
        log.info("Subscription 7-day expiry reminder queued for org: {}", org.getName());
    }

    /**
     * Send subscription expiring (3 days).
     */
    @Async
    public void sendSubscriptionExpiring3DaysEmail(Organization org) {
        String email = resolveOrgEmail(org);
        sendNotification(
                "subscription_expiring",
                email,
                org,
                NotificationPriority.HIGH,
                Map.of(
                        "orgName", org.getName(),
                        "daysLeft", 3,
                        "appName", appName,
                        "baseUrl", appBaseUrl
                )
        );
        log.info("Subscription 3-day expiry reminder queued for org: {}", org.getName());
    }

    /**
     * Send subscription expired notification.
     */
    @Async
    public void sendSubscriptionExpiredEmail(Organization org) {
        String email = resolveOrgEmail(org);
        sendNotification(
                "subscription_expired",
                email,
                org,
                NotificationPriority.HIGH,
                Map.of(
                        "orgName", org.getName(),
                        "appName", appName,
                        "baseUrl", appBaseUrl
                )
        );
        log.info("Subscription expired email queued for org: {}", org.getName());
    }

    // ==================== Admin Notifications ====================

    /**
     * Notify super admin when a new organization signs up.
     */
    @Async
    public void sendNewOrgNotificationToAdmin(String adminEmail, Organization org) {
        NotificationRequest request = NotificationRequest.builder()
                .channel(NotificationChannel.EMAIL)
                .templateName("new_org_signup_admin")
                .recipients(List.of(new Recipient(adminEmail, null, null, Map.of())))
                .parameters(Map.of(
                        "orgName", org.getName(),
                        "orgEmail", org.getEmailAddress(),
                        "appName", appName,
                        "baseUrl", appBaseUrl
                ))
                .priority(NotificationPriority.MEDIUM)
                .metadata(Map.of("eventType", "NEW_ORG_SIGNUP_ADMIN"))
                .build();

        try {
            String notificationId = notificationService.sendNotificationDirectly(request);
            log.info("New org admin notification queued [{}] for: {}", notificationId, adminEmail);
        } catch (Exception e) {
            log.error("Failed to queue new org admin notification to {}: {}", adminEmail, e.getMessage());
        }
    }

    /**
     * Send subscription cancelled notification.
     */
    @Async
    public void sendSubscriptionCancelledEmail(Organization org, String reason) {
        String email = resolveOrgEmail(org);
        sendNotification(
                "subscription_cancelled",
                email,
                org,
                NotificationPriority.MEDIUM,
                Map.of(
                        "orgName", org.getName(),
                        "reason", reason != null && !reason.isBlank() ? reason : "No reason provided",
                        "appName", appName,
                        "baseUrl", appBaseUrl
                )
        );
        log.info("Subscription cancelled email queued for org: {}", org.getName());
    }

    // ==================== Payment Notifications ====================

    /**
     * Send payment failed notification.
     */
    @Async
    public void sendPaymentFailedEmail(Organization org, String planName) {
        String email = resolveOrgEmail(org);
        sendNotification(
                "subscription_payment_failed",
                email,
                org,
                NotificationPriority.HIGH,
                Map.of(
                        "orgName", org.getName(),
                        "planName", planName,
                        "appName", appName,
                        "baseUrl", appBaseUrl
                )
        );
        log.info("Payment failed email queued for org: {}", org.getName());
    }

    // ==================== Internal Helpers ====================

    /**
     * Central method to build and send a notification via the notification module.
     */
    private void sendNotification(String templateName, String recipientEmail,
                                  Organization org, NotificationPriority priority,
                                  Map<String, Object> parameters) {
        NotificationRequest request = NotificationRequest.builder()
                .channel(NotificationChannel.EMAIL)
                .templateName(templateName)
                .recipients(List.of(new Recipient(recipientEmail, null, null, Map.of())))
                .parameters(parameters)
                .priority(priority)
                .tenantId(org.getId() != null ? org.getId().toString() : null)
                .metadata(Map.of("orgId", org.getId() != null ? org.getId().toString() : ""))
                .build();

        try {
            String notificationId = notificationService.sendNotificationDirectly(request);
            log.debug("Notification [{}] queued: template={}, recipient={}", notificationId, templateName, recipientEmail);
        } catch (Exception e) {
            log.error("Failed to queue notification: template={}, recipient={}, error={}",
                    templateName, recipientEmail, e.getMessage());
            // Don't rethrow - email failures shouldn't break business logic
        }
    }

    /**
     * Resolve the best email address for an organization.
     */
    private String resolveOrgEmail(Organization org) {
        return org.getContactEmail() != null ? org.getContactEmail() : org.getEmailAddress();
    }
}
