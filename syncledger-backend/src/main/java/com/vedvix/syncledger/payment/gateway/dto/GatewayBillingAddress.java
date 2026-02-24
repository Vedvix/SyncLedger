package com.vedvix.syncledger.payment.gateway.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Billing address details from a payment method.
 */
@Data
@Builder
public class GatewayBillingAddress {
    
    private String name;
    private String line1;
    private String line2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String phone;
    private String email;
}
