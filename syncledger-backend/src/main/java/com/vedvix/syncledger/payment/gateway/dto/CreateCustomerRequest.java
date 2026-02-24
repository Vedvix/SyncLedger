package com.vedvix.syncledger.payment.gateway.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Request DTO for creating a customer in the payment gateway.
 * Gateway-agnostic - works with any payment provider.
 */
@Data
@Builder
public class CreateCustomerRequest {
    
    /**
     * Internal user ID from our system
     */
    private Long userId;
    
    /**
     * Internal user UUID from our system
     */
    private String userUuid;
    
    /**
     * Customer email address
     */
    private String email;
    
    /**
     * Customer full name
     */
    private String name;
    
    /**
     * Customer phone number
     */
    private String phone;
    
    /**
     * Additional metadata to store with the customer
     */
    private Map<String, String> metadata;
}
