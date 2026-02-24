/**
 * Payment Module - Modular Monolith Architecture
 * 
 * <h2>Overview</h2>
 * This module handles all payment-related functionality for the Sipstr platform.
 * It follows modular monolith principles with clear boundaries and gateway abstraction.
 * 
 * <h2>Architecture</h2>
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────┐
 * │                      Payment Module                             │
 * ├─────────────────────────────────────────────────────────────────┤
 * │  ┌─────────────────────────────────────────────────────────┐   │
 * │  │                    API Layer (Facades)                   │   │
 * │  │  PaymentFacade │ PaymentMethodFacade │ StorePaymentFacade│   │
 * │  └─────────────────────────────────────────────────────────┘   │
 * │                            │                                    │
 * │  ┌─────────────────────────────────────────────────────────┐   │
 * │  │                    Service Layer                         │   │
 * │  │  PaymentProcessingService │ RefundProcessingService      │   │
 * │  │  PaymentMethodManagementService │ ConnectedAccountService│   │
 * │  │  CustomerManagementService                               │   │
 * │  └─────────────────────────────────────────────────────────┘   │
 * │                            │                                    │
 * │  ┌─────────────────────────────────────────────────────────┐   │
 * │  │              Gateway Abstraction Layer                   │   │
 * │  │           PaymentGateway (Interface)                     │   │
 * │  │                      │                                   │   │
 * │  │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐    │   │
 * │  │  │ Stripe  │  │ PayPal  │  │ Square  │  │ Future  │    │   │
 * │  │  │ Gateway │  │ Gateway │  │ Gateway │  │ Gateway │    │   │
 * │  │  └─────────┘  └─────────┘  └─────────┘  └─────────┘    │   │
 * │  └─────────────────────────────────────────────────────────┘   │
 * │                            │                                    │
 * │  ┌─────────────────────────────────────────────────────────┐   │
 * │  │              Webhook Processing Layer                    │   │
 * │  │  WebhookEventDispatcher → WebhookEventHandler(s)         │   │
 * │  └─────────────────────────────────────────────────────────┘   │
 * └─────────────────────────────────────────────────────────────────┘
 * </pre>
 * 
 * <h2>Package Structure</h2>
 * <ul>
 *   <li>{@code com.vedvix.syncledger.payment.api} - Public API facades (module boundary)</li>
 *   <li>{@code com.vedvix.syncledger.payment.gateway} - Gateway interface and implementations</li>
 *   <li>{@code com.vedvix.syncledger.payment.gateway.dto} - Gateway-agnostic DTOs</li>
 *   <li>{@code com.vedvix.syncledger.payment.gateway.stripe} - Stripe implementation</li>
 *   <li>{@code com.vedvix.syncledger.payment.gateway.exception} - Gateway exceptions</li>
 *   <li>{@code com.vedvix.syncledger.payment.service} - Internal service layer</li>
 *   <li>{@code com.vedvix.syncledger.payment.webhook} - Webhook event processing</li>
 *   <li>{@code com.vedvix.syncledger.payment.webhook.handlers} - Event handlers</li>
 *   <li>{@code com.vedvix.syncledger.payment.config} - Module configuration</li>
 * </ul>
 * 
 * <h2>Design Patterns Used</h2>
 * <ul>
 *   <li><b>Strategy Pattern</b>: PaymentGateway interface with multiple implementations</li>
 *   <li><b>Facade Pattern</b>: Public API through facade classes</li>
 *   <li><b>Factory Pattern</b>: PaymentGatewayFactory for gateway selection</li>
 *   <li><b>Observer Pattern</b>: WebhookEventHandler for event processing</li>
 *   <li><b>Chain of Responsibility</b>: WebhookEventDispatcher routing</li>
 * </ul>
 * 
 * <h2>Adding a New Payment Gateway</h2>
 * <ol>
 *   <li>Create implementation class implementing {@code PaymentGateway}</li>
 *   <li>Annotate with {@code @Component} and unique {@code @Qualifier}</li>
 *   <li>Add configuration properties in {@code PaymentModuleProperties}</li>
 *   <li>Map gateway-specific responses to gateway-agnostic DTOs</li>
 *   <li>No changes needed to service or facade layers!</li>
 * </ol>
 * 
 * <h2>Usage Examples</h2>
 * <pre>
 * // Using PaymentFacade for orders
 * {@literal @}Autowired PaymentFacade paymentFacade;
 * PaymentResult result = paymentFacade.createPaymentForOrder(userId, orderId, amount, customerId);
 * 
 * // Using PaymentMethodFacade for saved cards
 * {@literal @}Autowired PaymentMethodFacade paymentMethodFacade;
 * SetupIntentResult setup = paymentMethodFacade.createSetupIntent(userId, customerId);
 * 
 * // Using StorePaymentFacade for Connect
 * {@literal @}Autowired StorePaymentFacade storePaymentFacade;
 * ConnectedAccountResult account = storePaymentFacade.createConnectedAccount(storeId, email, businessName);
 * </pre>
 * 
 * @since 1.0.0
 * @see com.vedvix.syncledger.payment.api.PaymentFacade
 * @see com.vedvix.syncledger.payment.api.PaymentMethodFacade
 * @see com.vedvix.syncledger.payment.api.StorePaymentFacade
 * @see com.vedvix.syncledger.payment.gateway.PaymentGateway
 */
package com.vedvix.syncledger.payment;
