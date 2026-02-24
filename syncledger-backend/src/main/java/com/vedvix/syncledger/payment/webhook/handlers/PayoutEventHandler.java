package com.vedvix.syncledger.payment.webhook.handlers;

import com.vedvix.syncledger.payment.gateway.dto.GatewayWebhookEvent;
import com.vedvix.syncledger.payment.gateway.dto.GatewayWebhookEvent.NormalizedEventType;
import com.vedvix.syncledger.payment.webhook.WebhookEventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handles payout-related events from payment gateways.
 */
@Slf4j
@Component
public class PayoutEventHandler implements WebhookEventHandler {

    @Override
    public boolean canHandle(GatewayWebhookEvent event) {
        NormalizedEventType type = event.getNormalizedType();
        return type == NormalizedEventType.PAYOUT_CREATED ||
               type == NormalizedEventType.PAYOUT_PAID ||
               type == NormalizedEventType.PAYOUT_FAILED;
    }

    @Override
    public void handle(GatewayWebhookEvent event) {
        log.info("Processing payout event: {} ({})", event.getEventId(), event.getNormalizedType());
        
        String payoutId = event.getRelatedObjectId();
        NormalizedEventType eventType = event.getNormalizedType();
        
        switch (eventType) {
            case PAYOUT_CREATED -> handlePayoutCreated(payoutId, event);
            case PAYOUT_PAID -> handlePayoutPaid(payoutId, event);
            case PAYOUT_FAILED -> handlePayoutFailed(payoutId, event);
            default -> log.warn("Unexpected payout event type: {}", eventType);
        }
    }

    private void handlePayoutCreated(String payoutId, GatewayWebhookEvent event) {
        // TODO: Implement
        // 1. Log payout creation
        // 2. Create payout record if needed
        log.info("Payout created: {}", payoutId);
    }

    private void handlePayoutPaid(String payoutId, GatewayWebhookEvent event) {
        // TODO: Implement
        // 1. Find related store/account
        // 2. Update payout record to completed
        // 3. Notify store owner of successful payout
        
        java.util.Map<String, String> metadata = event.getMetadata();
        String amount = metadata != null ? metadata.get("amount") : null;
        String currency = metadata != null ? metadata.get("currency") : null;
        log.info("Payout {} paid: {} {}", payoutId, amount, currency);
    }

    private void handlePayoutFailed(String payoutId, GatewayWebhookEvent event) {
        // TODO: Implement
        // 1. Find related store/account
        // 2. Update payout record to failed
        // 3. Notify store owner of failed payout
        // 4. Alert operations team
        
        java.util.Map<String, String> metadata = event.getMetadata();
        String failureCode = metadata != null ? metadata.get("failure_code") : null;
        String failureMessage = metadata != null ? metadata.get("failure_message") : null;
        log.error("Payout {} failed: {} - {}", payoutId, failureCode, failureMessage);
    }

    @Override
    public int getPriority() {
        return 25;
    }
}
