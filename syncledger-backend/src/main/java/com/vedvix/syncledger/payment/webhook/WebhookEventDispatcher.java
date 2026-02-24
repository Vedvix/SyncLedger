package com.vedvix.syncledger.payment.webhook;

import com.vedvix.syncledger.payment.gateway.dto.GatewayWebhookEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Dispatcher for routing webhook events to appropriate handlers.
 * Uses the Chain of Responsibility pattern.
 */
@Slf4j
@Component
public class WebhookEventDispatcher {

    private final List<WebhookEventHandler> handlers;

    /**
     * Constructor - handlers are injected and sorted by priority.
     */
    public WebhookEventDispatcher(List<WebhookEventHandler> handlers) {
        this.handlers = handlers.stream()
                .sorted(Comparator.comparingInt(WebhookEventHandler::getPriority))
                .toList();
        
        log.info("WebhookEventDispatcher initialized with {} handlers", handlers.size());
    }

    /**
     * Dispatch an event to all capable handlers.
     * Events can be handled by multiple handlers if needed.
     *
     * @param event the webhook event to dispatch
     */
    @Async
    @Transactional
    public void dispatch(GatewayWebhookEvent event) {
        log.info("Dispatching webhook event: {} ({})", event.getEventId(), event.getEventType());
        
        boolean handled = false;
        
        for (WebhookEventHandler handler : handlers) {
            try {
                if (handler.canHandle(event)) {
                    log.debug("Handler {} processing event {}", 
                            handler.getClass().getSimpleName(), event.getEventId());
                    handler.handle(event);
                    handled = true;
                }
            } catch (Exception e) {
                log.error("Handler {} failed for event {}: {}", 
                        handler.getClass().getSimpleName(), event.getEventId(), e.getMessage(), e);
                // Continue to next handler - don't let one failure stop others
            }
        }
        
        if (!handled) {
            log.info("No handler found for event type: {}", event.getEventType());
        }
    }

    /**
     * Dispatch to the first handler that can handle the event.
     * Stops after first successful handling.
     *
     * @param event the webhook event
     * @return true if event was handled
     */
    @Async
    @Transactional
    public boolean dispatchToFirst(GatewayWebhookEvent event) {
        log.info("Dispatching webhook event (first match): {} ({})", 
                event.getEventId(), event.getEventType());
        
        for (WebhookEventHandler handler : handlers) {
            try {
                if (handler.canHandle(event)) {
                    handler.handle(event);
                    return true;
                }
            } catch (Exception e) {
                log.error("Handler {} failed for event {}: {}", 
                        handler.getClass().getSimpleName(), event.getEventId(), e.getMessage(), e);
                throw e; // Rethrow to mark as failed
            }
        }
        
        log.info("No handler found for event type: {}", event.getEventType());
        return false;
    }
}
