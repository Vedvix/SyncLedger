package com.vedvix.syncledger.payment.webhook;

import com.vedvix.syncledger.payment.gateway.dto.GatewayWebhookEvent;

/**
 * Interface for webhook event handlers.
 * Implement this interface for each type of webhook event you want to handle.
 *
 * <p>Usage:
 * <pre>
 * &#64;Component
 * public class PaymentSucceededHandler implements WebhookEventHandler {
 *     &#64;Override
 *     public boolean canHandle(GatewayWebhookEvent event) {
 *         return event.getNormalizedType() == NormalizedEventType.PAYMENT_SUCCEEDED;
 *     }
 *
 *     &#64;Override
 *     public void handle(GatewayWebhookEvent event) {
 *         // Process payment success
 *     }
 * }
 * </pre>
 */
public interface WebhookEventHandler {

    /**
     * Check if this handler can process the given event.
     *
     * @param event the webhook event
     * @return true if this handler should process the event
     */
    boolean canHandle(GatewayWebhookEvent event);

    /**
     * Process the webhook event.
     * This method is called asynchronously after verification.
     *
     * @param event the webhook event to process
     */
    void handle(GatewayWebhookEvent event);

    /**
     * Get the priority of this handler (lower = higher priority).
     * Default is 0. Use negative values for higher priority.
     *
     * @return the handler priority
     */
    default int getPriority() {
        return 0;
    }
}
