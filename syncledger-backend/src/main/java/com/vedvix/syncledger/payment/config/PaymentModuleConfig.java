package com.vedvix.syncledger.payment.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuration class for the Payment Module.
 * 
 * This module follows Modular Monolith architecture principles:
 * - Clear boundaries: All payment logic encapsulated in this module
 * - Gateway abstraction: PaymentGateway interface allows swapping providers
 * - Facade pattern: External access through facade classes only
 * - Event-driven: Webhook handling through dispatcher pattern
 * 
 * Module Structure:
 * - api/       : Public facades (PaymentFacade, PaymentMethodFacade, StorePaymentFacade)
 * - gateway/   : Gateway abstraction and implementations
 * - service/   : Internal service layer
 * - webhook/   : Webhook processing system
 * - config/    : Module configuration
 */
@Configuration
@EnableAsync
@ComponentScan(basePackages = {
    "com.vedvix.syncledger.payment.api",
    "com.vedvix.syncledger.payment.gateway",
    "com.vedvix.syncledger.payment.service",
    "com.vedvix.syncledger.payment.webhook",
    "com.vedvix.syncledger.payment.config"
})
public class PaymentModuleConfig {
    
    // Module constants
    public static final String MODULE_NAME = "payment";
    public static final String MODULE_VERSION = "1.0.0";
    
    // Default configuration values
    public static final int MAX_PAYMENT_METHODS_PER_CUSTOMER = 5;
    public static final int PAYMENT_INTENT_EXPIRY_MINUTES = 30;
    public static final String DEFAULT_CURRENCY = "usd";
}
