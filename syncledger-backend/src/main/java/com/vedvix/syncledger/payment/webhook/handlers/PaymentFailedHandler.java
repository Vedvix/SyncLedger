package com.vedvix.syncledger.payment.webhook.handlers;

import com.vedvix.syncledger.payment.gateway.dto.GatewayWebhookEvent;
import com.vedvix.syncledger.payment.gateway.dto.GatewayWebhookEvent.NormalizedEventType;
import com.vedvix.syncledger.payment.webhook.WebhookEventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handles payment failed events from payment gateways.
 */
@Slf4j
@Component
public class PaymentFailedHandler implements WebhookEventHandler {

    @Override
    public boolean canHandle(GatewayWebhookEvent event) {
        return event.getNormalizedType() == NormalizedEventType.PAYMENT_FAILED;
    }

    @Override
    public void handle(GatewayWebhookEvent event) {
        log.info("Processing payment failed event: {}", event.getEventId());
        
        String paymentIntentId = event.getRelatedObjectId();
        String failureReason = event.getMetadata() != null 
                ? event.getMetadata().get("failure_message") 
                : "Unknown";
        
        // TODO: Implement business logic
        // 1. Find order by payment intent ID
        // 2. Update order status to PAYMENT_FAILED
        // 3. Log failure reason
        // 4. Notify customer of payment failure
        // 5. Possibly trigger retry logic
        
        log.warn("Payment failed for intent {}: {}", paymentIntentId, failureReason);
    }

    @Override
    public int getPriority() {
        return 10;
    }
}
