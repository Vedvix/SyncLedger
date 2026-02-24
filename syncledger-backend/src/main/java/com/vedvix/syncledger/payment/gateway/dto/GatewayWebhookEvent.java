package com.vedvix.syncledger.payment.gateway.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Response DTO representing a parsed webhook event from the gateway.
 */
@Data
@Builder
public class GatewayWebhookEvent {
    
    /**
     * Gateway-specific event ID (e.g., evt_xxx for Stripe)
     */
    private String eventId;
    
    /**
     * Event type (e.g., "payment_intent.succeeded", "setup_intent.succeeded")
     */
    private String eventType;
    
    /**
     * Normalized event type for gateway-agnostic processing
     */
    private NormalizedEventType normalizedType;
    
    /**
     * The event data/payload as a map
     */
    private Map<String, Object> data;
    
    /**
     * Raw event object (gateway-specific)
     */
    private Object rawEvent;
    
    /**
     * Timestamp when event was created (epoch seconds)
     */
    private Long createdAt;
    
    /**
     * Whether verification was successful
     */
    private boolean verified;
    
    /**
     * Error message if verification failed
     */
    private String errorMessage;
    
    /**
     * Related object ID (e.g., payment_intent ID, refund ID)
     */
    private String relatedObjectId;
    
    /**
     * Additional metadata from the event
     */
    private Map<String, String> metadata;
    
    /**
     * Get the related object ID.
     * @return the related object ID
     */
    public String getRelatedObjectId() {
        return relatedObjectId;
    }
    
    /**
     * Get metadata from the event.
     * @return metadata map
     */
    public Map<String, String> getMetadata() {
        return metadata;
    }
    
    /**
     * Normalized event types for gateway-agnostic processing
     */
    public enum NormalizedEventType {
        PAYMENT_SUCCEEDED,
        PAYMENT_FAILED,
        PAYMENT_CANCELED,
        PAYMENT_REQUIRES_ACTION,
        
        SETUP_INTENT_SUCCEEDED,
        SETUP_SUCCEEDED,
        SETUP_FAILED,
        
        REFUND_CREATED,
        REFUND_SUCCEEDED,
        REFUND_FAILED,
        
        ACCOUNT_UPDATED,
        ACCOUNT_CAPABILITY_UPDATED,
        ACCOUNT_ONBOARDING_COMPLETE,
        
        TRANSFER_CREATED,
        TRANSFER_UPDATED,
        TRANSFER_REVERSED,
        
        PAYOUT_CREATED,
        PAYOUT_PAID,
        PAYOUT_FAILED,
        
        SUBSCRIPTION_CREATED,
        SUBSCRIPTION_UPDATED,
        SUBSCRIPTION_DELETED,
        
        INVOICE_PAID,
        INVOICE_PAYMENT_FAILED,
        
        CHECKOUT_SESSION_COMPLETED,
        
        UNKNOWN
    }
}
