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
 * Handles invoice webhook events from Stripe:
 * - invoice.paid (subscription payment succeeded)
 * - invoice.payment_failed (subscription payment failed)
 *
 * <p>Publishes Spring application events for the SubscriptionService
 * to update payment state and send notifications.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceEventHandler implements WebhookEventHandler {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public boolean canHandle(GatewayWebhookEvent event) {
        return event.getNormalizedType() == NormalizedEventType.INVOICE_PAID
                || event.getNormalizedType() == NormalizedEventType.INVOICE_PAYMENT_FAILED;
    }

    @Override
    public void handle(GatewayWebhookEvent event) {
        log.info("Processing invoice event: {} ({})", event.getEventType(), event.getEventId());

        try {
            if (event.getRawEvent() instanceof com.stripe.model.Event stripeEvent
                    && stripeEvent.getData() != null
                    && stripeEvent.getData().getObject() instanceof com.stripe.model.Invoice invoice) {

                String invoiceId = invoice.getId();
                String subscriptionId = invoice.getSubscription();
                String customerId = invoice.getCustomer();
                Long amountPaid = invoice.getAmountPaid();
                String status = invoice.getStatus();
                Map<String, String> metadata = invoice.getMetadata();

                log.info("Invoice {} for subscription {} status: {} amount: {}",
                        invoiceId, subscriptionId, status, amountPaid);

                eventPublisher.publishEvent(new InvoiceWebhookEvent(
                        this,
                        event.getNormalizedType(),
                        invoiceId,
                        subscriptionId,
                        customerId,
                        amountPaid,
                        status,
                        metadata
                ));

                log.info("Invoice webhook event published for invoice: {}", invoiceId);
            } else {
                log.warn("Could not extract Stripe Invoice from webhook event: {}", event.getEventId());
            }
        } catch (Exception e) {
            log.error("Error processing invoice event {}: {}", event.getEventId(), e.getMessage(), e);
        }
    }

    @Override
    public int getPriority() {
        return 0;
    }

    /**
     * Spring application event for invoice webhook notifications.
     */
    public static class InvoiceWebhookEvent extends org.springframework.context.ApplicationEvent {

        private final NormalizedEventType eventType;
        private final String invoiceId;
        private final String subscriptionId;
        private final String customerId;
        private final Long amountPaid;
        private final String status;
        private final Map<String, String> metadata;

        public InvoiceWebhookEvent(Object source, NormalizedEventType eventType,
                                   String invoiceId, String subscriptionId,
                                   String customerId, Long amountPaid,
                                   String status, Map<String, String> metadata) {
            super(source);
            this.eventType = eventType;
            this.invoiceId = invoiceId;
            this.subscriptionId = subscriptionId;
            this.customerId = customerId;
            this.amountPaid = amountPaid;
            this.status = status;
            this.metadata = metadata;
        }

        public NormalizedEventType getEventType() { return eventType; }
        public String getInvoiceId() { return invoiceId; }
        public String getSubscriptionId() { return subscriptionId; }
        public String getCustomerId() { return customerId; }
        public Long getAmountPaid() { return amountPaid; }
        public String getStatus() { return status; }
        public Map<String, String> getMetadata() { return metadata; }
    }
}
