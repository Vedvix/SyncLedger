package com.vedvix.syncledger.payment.webhook.handlers;

import com.vedvix.syncledger.payment.gateway.dto.GatewayWebhookEvent;
import com.vedvix.syncledger.payment.gateway.dto.GatewayWebhookEvent.NormalizedEventType;
import com.vedvix.syncledger.payment.webhook.WebhookEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Handles subscription lifecycle webhook events from Stripe:
 * - customer.subscription.created
 * - customer.subscription.updated
 * - customer.subscription.deleted
 *
 * <p>Publishes Spring application events that the SubscriptionService can listen to
 * for updating local subscription state (status, dates, etc.).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionEventHandler implements WebhookEventHandler {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public boolean canHandle(GatewayWebhookEvent event) {
        return event.getNormalizedType() == NormalizedEventType.SUBSCRIPTION_CREATED
                || event.getNormalizedType() == NormalizedEventType.SUBSCRIPTION_UPDATED
                || event.getNormalizedType() == NormalizedEventType.SUBSCRIPTION_DELETED;
    }

    @Override
    public void handle(GatewayWebhookEvent event) {
        log.info("Processing subscription event: {} ({})", event.getEventType(), event.getEventId());

        try {
            if (event.getRawEvent() instanceof com.stripe.model.Event stripeEvent
                    && stripeEvent.getData() != null
                    && stripeEvent.getData().getObject() instanceof com.stripe.model.Subscription subscription) {

                String subscriptionId = subscription.getId();
                String customerId = subscription.getCustomer();
                String status = subscription.getStatus();
                Long periodEnd = subscription.getCurrentPeriodEnd();
                Map<String, String> metadata = subscription.getMetadata();

                log.info("Subscription {} for customer {} status: {}", subscriptionId, customerId, status);

                // Publish a Spring event for SubscriptionService to handle
                eventPublisher.publishEvent(new SubscriptionWebhookEvent(
                        this,
                        event.getNormalizedType(),
                        subscriptionId,
                        customerId,
                        status,
                        periodEnd,
                        subscription.getCancelAtPeriodEnd(),
                        metadata
                ));

                log.info("Subscription webhook event published for sub: {}", subscriptionId);
            } else {
                log.warn("Could not extract Stripe Subscription from webhook event: {}", event.getEventId());
            }
        } catch (Exception e) {
            log.error("Error processing subscription event {}: {}", event.getEventId(), e.getMessage(), e);
        }
    }

    @Override
    public int getPriority() {
        return 0;
    }

    /**
     * Spring application event for subscription webhook notifications.
     * The SubscriptionService listens for these events to update local state.
     */
    public static class SubscriptionWebhookEvent extends org.springframework.context.ApplicationEvent {

        private final NormalizedEventType eventType;
        private final String subscriptionId;
        private final String customerId;
        private final String status;
        private final Long currentPeriodEnd;
        private final Boolean cancelAtPeriodEnd;
        private final Map<String, String> metadata;

        public SubscriptionWebhookEvent(Object source, NormalizedEventType eventType,
                                        String subscriptionId, String customerId,
                                        String status, Long currentPeriodEnd,
                                        Boolean cancelAtPeriodEnd, Map<String, String> metadata) {
            super(source);
            this.eventType = eventType;
            this.subscriptionId = subscriptionId;
            this.customerId = customerId;
            this.status = status;
            this.currentPeriodEnd = currentPeriodEnd;
            this.cancelAtPeriodEnd = cancelAtPeriodEnd;
            this.metadata = metadata;
        }

        public NormalizedEventType getEventType() { return eventType; }
        public String getSubscriptionId() { return subscriptionId; }
        public String getCustomerId() { return customerId; }
        public String getStatus() { return status; }
        public Long getCurrentPeriodEnd() { return currentPeriodEnd; }
        public Boolean getCancelAtPeriodEnd() { return cancelAtPeriodEnd; }
        public Map<String, String> getMetadata() { return metadata; }
    }
}
