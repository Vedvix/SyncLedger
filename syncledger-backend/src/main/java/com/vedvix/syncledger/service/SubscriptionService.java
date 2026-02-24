package com.vedvix.syncledger.service;

import com.vedvix.syncledger.dto.AdminChangePlanRequest;
import com.vedvix.syncledger.dto.SubscriptionDTO;
import com.vedvix.syncledger.dto.UpgradeSubscriptionRequest;
import com.vedvix.syncledger.exception.BadRequestException;
import com.vedvix.syncledger.exception.ResourceNotFoundException;
import com.vedvix.syncledger.model.*;
import com.vedvix.syncledger.payment.api.SubscriptionPaymentFacade;
import com.vedvix.syncledger.payment.config.PaymentModuleProperties;
import com.vedvix.syncledger.payment.gateway.dto.GatewayCheckoutSession;
import com.vedvix.syncledger.payment.gateway.dto.GatewayCustomer;
import com.vedvix.syncledger.payment.gateway.dto.GatewaySubscription;
import com.vedvix.syncledger.payment.webhook.handlers.CheckoutSessionCompletedHandler.CheckoutCompletedEvent;
import com.vedvix.syncledger.payment.webhook.handlers.InvoiceEventHandler.InvoiceWebhookEvent;
import com.vedvix.syncledger.payment.webhook.handlers.SubscriptionEventHandler.SubscriptionWebhookEvent;
import com.vedvix.syncledger.repository.OrganizationRepository;
import com.vedvix.syncledger.repository.SubscriptionAuditLogRepository;
import com.vedvix.syncledger.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * Service managing subscription lifecycle: trial creation, plan upgrades via Stripe,
 * cancellation, and status transitions.
 *
 * <p>Integrates with the Payment Module via {@link SubscriptionPaymentFacade} for:
 * <ul>
 *   <li>Creating Stripe customers for organizations</li>
 *   <li>Creating Checkout Sessions for subscription payment</li>
 *   <li>Creating/cancelling Stripe subscriptions</li>
 * </ul>
 *
 * <p>Listens for webhook events from the Payment Module's webhook handlers
 * to synchronize local subscription state with Stripe.
 *
 * @author vedvix
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private static final int TRIAL_DAYS = 15;

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionAuditLogRepository auditLogRepository;
    private final OrganizationRepository organizationRepository;
    private final SubscriptionPaymentFacade paymentFacade;
    private final SubscriptionEmailService emailService;
    private final PaymentModuleProperties paymentProperties;

    @Value("${app.base-url:http://localhost:5173}")
    private String appBaseUrl;

    /**
     * Create a trial subscription for a newly signed-up organization.
     */
    @Transactional
    public Subscription createTrialSubscription(Organization organization) {
        log.info("Creating trial subscription for organization: {}", organization.getName());

        // Ensure no duplicate subscription
        subscriptionRepository.findByOrganization_Id(organization.getId()).ifPresent(s -> {
            throw new BadRequestException("Organization already has a subscription");
        });

        LocalDateTime now = LocalDateTime.now();
        Subscription subscription = Subscription.builder()
                .organization(organization)
                .plan(SubscriptionPlan.TRIAL)
                .status(SubscriptionStatus.TRIAL)
                .trialStartDate(now)
                .trialEndDate(now.plusDays(TRIAL_DAYS))
                .billingCycle(BillingCycle.MONTHLY)
                .priceCents(0L)
                .currency("USD")
                .build();

        subscription = subscriptionRepository.save(subscription);

        // Audit log
        logAuditEvent(organization.getId(), subscription.getId(),
                "TRIAL_STARTED", null, "TRIAL", null, "TRIAL", null);

        log.info("Trial subscription created for org {} (expires: {})",
                organization.getName(), subscription.getTrialEndDate());

        return subscription;
    }

    /**
     * Get subscription for an organization.
     */
    @Transactional(readOnly = true)
    public SubscriptionDTO getSubscription(Long organizationId) {
        Subscription subscription = subscriptionRepository.findByOrganization_Id(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "organizationId", organizationId));
        return mapToDTO(subscription);
    }

    /**
     * Upgrade from trial (or change plan) to a paid subscription.
     * Creates a Stripe Checkout Session for payment and returns the checkout URL.
     * The actual activation happens when we receive the checkout.session.completed webhook.
     */
    @Transactional
    public SubscriptionDTO upgradeSubscription(Long organizationId, UpgradeSubscriptionRequest request, Long performedBy) {
        Subscription subscription = subscriptionRepository.findByOrganization_Id(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "organizationId", organizationId));

        SubscriptionPlan newPlan;
        try {
            newPlan = SubscriptionPlan.valueOf(request.getPlan().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid plan: " + request.getPlan());
        }

        if (newPlan == SubscriptionPlan.TRIAL) {
            throw new BadRequestException("Cannot upgrade to trial plan");
        }

        // Check if Stripe is configured before attempting payment
        if (!isStripeConfigured()) {
            throw new BadRequestException(
                    "Payment processing is not configured. Please contact your administrator to set up Stripe integration.");
        }

        Organization org = subscription.getOrganization();

        // Ensure Stripe customer exists
        String stripeCustomerId = subscription.getStripeCustomerId();
        if (stripeCustomerId == null || stripeCustomerId.isBlank()) {
            String email = org.getContactEmail() != null ? org.getContactEmail() : org.getEmailAddress();
            GatewayCustomer customer = paymentFacade.createCustomerForOrganization(
                    email, org.getName(), org.getId());
            stripeCustomerId = customer.getId();
            subscription.setStripeCustomerId(stripeCustomerId);
        }

        // Determine billing cycle
        BillingCycle cycle = "ANNUAL".equalsIgnoreCase(request.getBillingCycle())
                ? BillingCycle.ANNUAL : BillingCycle.MONTHLY;

        // Map plan to Stripe price ID
        String priceId = resolveStripePriceId(newPlan, request.getBillingCycle());

        // Create Stripe Checkout Session
        String successUrl = appBaseUrl + "/subscription?session_id={CHECKOUT_SESSION_ID}&success=true";
        String cancelUrl = appBaseUrl + "/subscription?canceled=true";

        GatewayCheckoutSession checkoutSession = paymentFacade.createCheckoutSession(
                stripeCustomerId, priceId, org.getId(), successUrl, cancelUrl);

        // Store pending upgrade info so we can activate the correct plan on webhook
        String oldPlan = subscription.getPlan().name();
        String oldStatus = subscription.getStatus().name();
        subscription.setPendingPlan(newPlan);
        subscription.setPendingBillingCycle(cycle);
        subscriptionRepository.save(subscription);

        logAuditEvent(organizationId, subscription.getId(),
                "UPGRADE_INITIATED", oldStatus, oldStatus, oldPlan, newPlan.name(), performedBy);

        log.info("Checkout session created for org {} upgrade to {} ({}): {}",
                org.getName(), newPlan, cycle, checkoutSession.getSessionId());

        // Return DTO with checkout URL
        SubscriptionDTO dto = mapToDTO(subscription);
        dto.setCheckoutUrl(checkoutSession.getUrl());
        dto.setCheckoutSessionId(checkoutSession.getSessionId());
        return dto;
    }

    /**
     * Check if Stripe payment gateway is properly configured.
     */
    private boolean isStripeConfigured() {
        String apiKey = paymentProperties.getStripe().getApiKey();
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Resolve the Stripe Price ID for a given plan and billing cycle.
     * Reads from sipstr.payment.stripe.prices map in application configuration.
     * Key format: {plan}_{cycle} (e.g., "starter_monthly", "professional_annual")
     */
    private String resolveStripePriceId(SubscriptionPlan plan, String billingCycle) {
        String cycle = (billingCycle != null && billingCycle.equalsIgnoreCase("ANNUAL")) ? "annual" : "monthly";
        String key = plan.name().toLowerCase() + "_" + cycle;

        var prices = paymentProperties.getStripe().getPrices();
        String priceId = prices.get(key);
        if (priceId == null || priceId.isBlank()) {
            throw new BadRequestException(
                    String.format("No Stripe price configured for plan '%s' with billing cycle '%s'. "
                            + "Add sipstr.payment.stripe.prices.%s in application.yml", plan.name(), cycle, key));
        }
        return priceId;
    }

    /**
     * Cancel a subscription. If there's an active Stripe subscription,
     * it will be cancelled at the end of the current billing period.
     */
    @Transactional
    public SubscriptionDTO cancelSubscription(Long organizationId, String reason, Long performedBy) {
        Subscription subscription = subscriptionRepository.findByOrganization_Id(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "organizationId", organizationId));

        if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new BadRequestException("Subscription is already cancelled");
        }

        // Cancel in Stripe if there's an active subscription
        String stripeSubId = subscription.getStripeSubscriptionId();
        if (stripeSubId != null && !stripeSubId.isBlank()) {
            try {
                paymentFacade.cancelSubscription(stripeSubId, true); // cancel at period end
                log.info("Stripe subscription {} cancelled at period end", stripeSubId);
            } catch (Exception e) {
                log.warn("Failed to cancel Stripe subscription {} (proceeding with local cancel): {}",
                        stripeSubId, e.getMessage());
            }
        }

        String oldStatus = subscription.getStatus().name();
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancelledAt(LocalDateTime.now());
        subscription.setCancellationReason(reason);
        subscriptionRepository.save(subscription);

        // Send cancellation email
        Organization org = subscription.getOrganization();
        emailService.sendSubscriptionCancelledEmail(org, reason);

        logAuditEvent(organizationId, subscription.getId(),
                "SUBSCRIPTION_CANCELLED", oldStatus, "CANCELLED",
                subscription.getPlan().name(), subscription.getPlan().name(), performedBy);

        log.info("Subscription cancelled for org {}", organizationId);

        return mapToDTO(subscription);
    }

    /**
     * Expire a trial subscription (called by scheduler).
     */
    @Transactional
    public void expireTrialSubscription(Subscription subscription) {
        String oldStatus = subscription.getStatus().name();
        subscription.setStatus(SubscriptionStatus.EXPIRED);
        subscriptionRepository.save(subscription);

        Organization org = subscription.getOrganization();
        org.setStatus(OrganizationStatus.SUSPENDED);
        organizationRepository.save(org);

        logAuditEvent(org.getId(), subscription.getId(),
                "TRIAL_EXPIRED", oldStatus, "EXPIRED",
                "TRIAL", "TRIAL", null);

        log.info("Trial expired for org: {}", org.getName());
    }

    /**
     * Expire an active subscription (called by scheduler).
     */
    @Transactional
    public void expireActiveSubscription(Subscription subscription) {
        String oldStatus = subscription.getStatus().name();
        String oldPlan = subscription.getPlan().name();
        subscription.setStatus(SubscriptionStatus.EXPIRED);
        subscriptionRepository.save(subscription);

        Organization org = subscription.getOrganization();
        org.setStatus(OrganizationStatus.SUSPENDED);
        organizationRepository.save(org);

        logAuditEvent(org.getId(), subscription.getId(),
                "SUBSCRIPTION_EXPIRED", oldStatus, "EXPIRED", oldPlan, oldPlan, null);

        log.info("Subscription expired for org: {}", org.getName());
    }

    /**
     * Reactivate a suspended/expired subscription (super admin action).
     */
    @Transactional
    public SubscriptionDTO reactivateSubscription(Long organizationId, int additionalDays, Long performedBy) {
        Subscription subscription = subscriptionRepository.findByOrganization_Id(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "organizationId", organizationId));

        String oldStatus = subscription.getStatus().name();
        LocalDateTime now = LocalDateTime.now();

        if (subscription.getPlan() == SubscriptionPlan.TRIAL) {
            subscription.setStatus(SubscriptionStatus.TRIAL);
            subscription.setTrialEndDate(now.plusDays(additionalDays));
        } else {
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setSubscriptionEndDate(now.plusDays(additionalDays));
        }
        subscriptionRepository.save(subscription);

        Organization org = subscription.getOrganization();
        org.setStatus(subscription.getPlan() == SubscriptionPlan.TRIAL ?
                OrganizationStatus.TRIAL : OrganizationStatus.ACTIVE);
        organizationRepository.save(org);

        logAuditEvent(organizationId, subscription.getId(),
                "SUBSCRIPTION_REACTIVATED", oldStatus, subscription.getStatus().name(),
                subscription.getPlan().name(), subscription.getPlan().name(), performedBy);

        log.info("Subscription reactivated for org {} with {} additional days", organizationId, additionalDays);

        return mapToDTO(subscription);
    }

    /**
     * Admin-change an organization's subscription plan directly (bypasses Stripe).
     * Used by super admins to grant, swap, or override plans.
     */
    @Transactional
    public SubscriptionDTO adminChangePlan(Long organizationId, AdminChangePlanRequest request, Long performedBy) {
        Subscription subscription = subscriptionRepository.findByOrganization_Id(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "organizationId", organizationId));

        SubscriptionPlan newPlan;
        try {
            newPlan = SubscriptionPlan.valueOf(request.getPlan().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid plan: " + request.getPlan());
        }

        String oldPlan = subscription.getPlan().name();
        String oldStatus = subscription.getStatus().name();
        LocalDateTime now = LocalDateTime.now();

        int durationDays = request.getDurationDays() != null ? request.getDurationDays() : 365;
        BillingCycle cycle = "ANNUAL".equalsIgnoreCase(request.getBillingCycle())
                ? BillingCycle.ANNUAL : BillingCycle.MONTHLY;

        if (newPlan == SubscriptionPlan.TRIAL) {
            subscription.setPlan(SubscriptionPlan.TRIAL);
            subscription.setStatus(SubscriptionStatus.TRIAL);
            subscription.setTrialStartDate(now);
            subscription.setTrialEndDate(now.plusDays(durationDays));
        } else {
            subscription.setPlan(newPlan);
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setBillingCycle(cycle);
            subscription.setSubscriptionStartDate(now);
            subscription.setSubscriptionEndDate(now.plusDays(durationDays));
        }
        subscriptionRepository.save(subscription);

        // Update organization status accordingly
        Organization org = subscription.getOrganization();
        org.setStatus(newPlan == SubscriptionPlan.TRIAL
                ? OrganizationStatus.TRIAL : OrganizationStatus.ACTIVE);
        organizationRepository.save(org);

        logAuditEvent(organizationId, subscription.getId(),
                "ADMIN_PLAN_CHANGE", oldStatus, subscription.getStatus().name(),
                oldPlan, newPlan.name(), performedBy);

        log.info("Admin changed plan for org {} from {} to {} ({} days, performed by user {})",
                organizationId, oldPlan, newPlan.name(), durationDays, performedBy);

        return mapToDTO(subscription);
    }

    // ==================== Webhook Event Listeners ====================

    /**
     * Handle checkout.session.completed event from Stripe.
     * This fires when a customer completes the Checkout Session payment.
     * Activates the subscription locally.
     */
    @EventListener
    @Async
    @Transactional
    public void handleCheckoutCompleted(CheckoutCompletedEvent event) {
        log.info("Handling checkout completed: session={}, sub={}, customer={}",
                event.getSessionId(), event.getSubscriptionId(), event.getCustomerId());

        Map<String, String> metadata = event.getMetadata();
        if (metadata == null || !metadata.containsKey("organization_id")) {
            log.warn("Checkout session {} missing organization_id in metadata", event.getSessionId());
            return;
        }

        Long orgId;
        try {
            orgId = Long.parseLong(metadata.get("organization_id"));
        } catch (NumberFormatException e) {
            log.error("Invalid organization_id in checkout metadata: {}", metadata.get("organization_id"));
            return;
        }

        Subscription subscription = subscriptionRepository.findByOrganization_Id(orgId).orElse(null);
        if (subscription == null) {
            log.warn("No subscription found for org {} from checkout session", orgId);
            return;
        }

        // Retrieve the actual Stripe subscription to get plan details
        GatewaySubscription stripeSub = paymentFacade.retrieveSubscription(event.getSubscriptionId());
        if (!stripeSub.isSuccess()) {
            log.error("Failed to retrieve Stripe subscription {} for checkout", event.getSubscriptionId());
            return;
        }

        String oldStatus = subscription.getStatus().name();
        String oldPlan = subscription.getPlan().name();

        // Update local subscription with Stripe data
        subscription.setStripeCustomerId(event.getCustomerId());
        subscription.setStripeSubscriptionId(event.getSubscriptionId());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setSubscriptionStartDate(LocalDateTime.now());

        if (stripeSub.getCurrentPeriodEnd() != null) {
            subscription.setSubscriptionEndDate(
                    LocalDateTime.ofInstant(Instant.ofEpochSecond(stripeSub.getCurrentPeriodEnd()), ZoneId.systemDefault()));
        } else {
            subscription.setSubscriptionEndDate(LocalDateTime.now().plusMonths(1));
        }

        // Activate the plan that was stored during upgradeSubscription()
        if (subscription.getPendingPlan() != null) {
            subscription.setPlan(subscription.getPendingPlan());
            if (subscription.getPendingBillingCycle() != null) {
                subscription.setBillingCycle(subscription.getPendingBillingCycle());
            }
            // Clear pending fields
            subscription.setPendingPlan(null);
            subscription.setPendingBillingCycle(null);
        } else if (subscription.getPlan() == SubscriptionPlan.TRIAL) {
            // Fallback: if somehow no pending plan was stored
            subscription.setPlan(SubscriptionPlan.STARTER);
            log.warn("No pending plan found for org {} checkout; defaulting to STARTER", orgId);
        }
        subscription.setPriceCents(subscription.getPlan().getPriceInCents());

        subscriptionRepository.save(subscription);

        // Activate organization
        Organization org = subscription.getOrganization();
        org.setStatus(OrganizationStatus.ACTIVE);
        org.setSubscriptionPlan(subscription.getPlan().name());
        org.setSubscriptionExpiresAt(subscription.getSubscriptionEndDate());
        organizationRepository.save(org);

        // Send activation email
        emailService.sendSubscriptionActivatedEmail(org, subscription);

        logAuditEvent(orgId, subscription.getId(),
                "PLAN_UPGRADED", oldStatus, "ACTIVE", oldPlan, subscription.getPlan().name(), null);

        log.info("Subscription activated via checkout for org {}: plan={}, expires={}",
                org.getName(), subscription.getPlan(), subscription.getSubscriptionEndDate());
    }

    /**
     * Handle subscription lifecycle events from Stripe webhooks.
     * Updates local state to match Stripe subscription status.
     */
    @EventListener
    @Async
    @Transactional
    public void handleSubscriptionWebhookEvent(SubscriptionWebhookEvent event) {
        log.info("Handling subscription webhook: sub={}, status={}, type={}",
                event.getSubscriptionId(), event.getStatus(), event.getEventType());

        Subscription subscription = subscriptionRepository
                .findByStripeSubscriptionId(event.getSubscriptionId())
                .orElse(null);

        if (subscription == null) {
            log.warn("No local subscription found for Stripe subscription: {}", event.getSubscriptionId());
            return;
        }

        String stripeStatus = event.getStatus();
        Organization org = subscription.getOrganization();

        switch (stripeStatus) {
            case "active" -> {
                if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
                    subscription.setStatus(SubscriptionStatus.ACTIVE);
                    org.setStatus(OrganizationStatus.ACTIVE);
                    if (event.getCurrentPeriodEnd() != null) {
                        subscription.setSubscriptionEndDate(
                                LocalDateTime.ofInstant(Instant.ofEpochSecond(event.getCurrentPeriodEnd()),
                                        ZoneId.systemDefault()));
                        org.setSubscriptionExpiresAt(subscription.getSubscriptionEndDate());
                    }
                }
            }
            case "past_due" -> {
                log.warn("Subscription {} is past due for org {}", event.getSubscriptionId(), org.getName());
                // Keep active but flag
                subscription.setStatus(SubscriptionStatus.PAST_DUE);
            }
            case "canceled" -> {
                subscription.setStatus(SubscriptionStatus.CANCELLED);
                subscription.setCancelledAt(LocalDateTime.now());
                org.setStatus(OrganizationStatus.SUSPENDED);
            }
            case "unpaid" -> {
                subscription.setStatus(SubscriptionStatus.EXPIRED);
                org.setStatus(OrganizationStatus.SUSPENDED);
            }
            default -> log.debug("Unhandled Stripe subscription status: {}", stripeStatus);
        }

        subscriptionRepository.save(subscription);
        organizationRepository.save(org);
    }

    /**
     * Handle invoice events from Stripe webhooks.
     * Sends payment success/failure notifications.
     */
    @EventListener
    @Async
    @Transactional
    public void handleInvoiceWebhookEvent(InvoiceWebhookEvent event) {
        log.info("Handling invoice webhook: invoice={}, sub={}, status={}",
                event.getInvoiceId(), event.getSubscriptionId(), event.getStatus());

        if (event.getSubscriptionId() == null) {
            log.debug("Invoice {} is not subscription-related, skipping", event.getInvoiceId());
            return;
        }

        Subscription subscription = subscriptionRepository
                .findByStripeSubscriptionId(event.getSubscriptionId())
                .orElse(null);

        if (subscription == null) {
            log.warn("No local subscription found for invoice subscription: {}", event.getSubscriptionId());
            return;
        }

        Organization org = subscription.getOrganization();

        switch (event.getEventType()) {
            case INVOICE_PAID -> {
                log.info("Invoice paid for org {}: amount={}", org.getName(), event.getAmountPaid());
                // Subscription renewal is handled by subscription.updated webhook
            }
            case INVOICE_PAYMENT_FAILED -> {
                log.warn("Invoice payment failed for org {}", org.getName());
                emailService.sendPaymentFailedEmail(org, subscription.getPlan().getDisplayName());
            }
            default -> log.debug("Unhandled invoice event type: {}", event.getEventType());
        }
    }

    // ==================== Helpers ====================

    public SubscriptionDTO mapToDTO(Subscription s) {
        return SubscriptionDTO.builder()
                .id(s.getId())
                .organizationId(s.getOrganization().getId())
                .organizationName(s.getOrganization().getName())
                .plan(s.getPlan().name())
                .planDisplayName(s.getPlan().getDisplayName())
                .status(s.getStatus().name())
                .statusDisplayName(s.getStatus().getDisplayName())
                .trialStartDate(s.getTrialStartDate())
                .trialEndDate(s.getTrialEndDate())
                .remainingTrialDays(s.getRemainingTrialDays())
                .subscriptionStartDate(s.getSubscriptionStartDate())
                .subscriptionEndDate(s.getSubscriptionEndDate())
                .billingCycle(s.getBillingCycle() != null ? s.getBillingCycle().name() : null)
                .priceCents(s.getPriceCents())
                .currency(s.getCurrency())
                .stripeCustomerId(s.getStripeCustomerId())
                .stripeSubscriptionId(s.getStripeSubscriptionId())
                .cancelledAt(s.getCancelledAt())
                .cancellationReason(s.getCancellationReason())
                .hasAccess(s.hasAccess())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private void logAuditEvent(Long orgId, Long subId, String eventType,
                               String oldStatus, String newStatus,
                               String oldPlan, String newPlan, Long performedBy) {
        SubscriptionAuditLog auditLog = SubscriptionAuditLog.builder()
                .organizationId(orgId)
                .subscriptionId(subId)
                .eventType(eventType)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .oldPlan(oldPlan)
                .newPlan(newPlan)
                .performedBy(performedBy)
                .build();
        auditLogRepository.save(auditLog);
    }
}
