package com.vedvix.syncledger.payment.gateway.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Request DTO for updating a customer in the payment gateway.
 */
@Data
@Builder
public class UpdateCustomerRequest {
    
    private String email;
    private String name;
    private String phone;
    private Map<String, String> metadata;
}
