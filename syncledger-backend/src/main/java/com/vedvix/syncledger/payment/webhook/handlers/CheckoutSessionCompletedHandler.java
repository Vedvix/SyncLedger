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
 * Handles checkout.session.completed webhook events from Stripe.
 * This fires when a customer completes the Stripe Checkout flow.
 *
 * <p>Publishes a Spring application event for the SubscriptionService
 * to finalize the subscription activation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CheckoutSessionCompletedHandler implements WebhookEventHandler {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public boolean canHandle(GatewayWebhookEvent event) {
        return event.getNormalizedType() == NormalizedEventType.CHECKOUT_SESSION_COMPLETED;
    }

    @Override
    public void handle(GatewayWebhookEvent event) {
        log.info("Processing checkout session completed event: {}", event.getEventId());

        try {
            if (event.getRawEvent() instanceof com.stripe.model.Event stripeEvent
                    && stripeEvent.getData() != null
                    && stripeEvent.getData().getObject() instanceof com.stripe.model.checkout.Session session) {

                String sessionId = session.getId();
                String subscriptionId = session.getSubscription();
                String customerId = session.getCustomer();
                String mode = session.getMode();
                Map<String, String> metadata = session.getMetadata();

                log.info("Checkout completed: session={}, sub={}, customer={}, mode={}",
                        sessionId, subscriptionId, customerId, mode);

                // Only process subscription checkout sessions
                if ("subscription".equals(mode)) {
                    eventPublisher.publishEvent(new CheckoutCompletedEvent(
                            this,
                            sessionId,
                            subscriptionId,
                            customerId,
                            metadata
                    ));
                    log.info("Checkout completed event published for session: {}", sessionId);
                } else {
                    log.debug("Ignoring non-subscription checkout session: {}", sessionId);
                }
            } else {
                log.warn("Could not extract Stripe Session from webhook event: {}", event.getEventId());
            }
        } catch (Exception e) {
            log.error("Error processing checkout session event {}: {}", event.getEventId(), e.getMessage(), e);
        }
    }

    @Override
    public int getPriority() {
        return -1; // Higher priority than subscription events
    }

    /**
     * Spring application event for checkout session completion.
     */
    public static class CheckoutCompletedEvent extends org.springframework.context.ApplicationEvent {

        private final String sessionId;
        private final String subscriptionId;
        private final String customerId;
        private final Map<String, String> metadata;

        public CheckoutCompletedEvent(Object source, String sessionId,
                                      String subscriptionId, String customerId,
                                      Map<String, String> metadata) {
            super(source);
            this.sessionId = sessionId;
            this.subscriptionId = subscriptionId;
            this.customerId = customerId;
            this.metadata = metadata;
        }

        public String getSessionId() { return sessionId; }
        public String getSubscriptionId() { return subscriptionId; }
        public String getCustomerId() { return customerId; }
        public Map<String, String> getMetadata() { return metadata; }
    }
}
