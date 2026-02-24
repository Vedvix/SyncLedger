package com.vedvix.syncledger.payment.gateway;

import com.vedvix.syncledger.payment.gateway.dto.*;

import java.util.List;
import java.util.Map;

/**
 * Payment Gateway abstraction - implement this interface for each payment provider.
 * This enables the modular monolith to support multiple payment gateways (Stripe, PayPal, etc.)
 * using the Strategy Pattern.
 *
 * <p>To add a new payment gateway:
 * <ol>
 *   <li>Create a new class implementing this interface</li>
 *   <li>Annotate with @Service</li>
 *   <li>Use @ConditionalOnProperty or @Primary for gateway selection</li>
 * </ol>
 *
 * @see com.vedvix.syncledger.payment.gateway.stripe.StripePaymentGateway
 */
public interface PaymentGateway {

    // ========== Gateway Identification ==========

    /**
     * Returns the unique identifier for this gateway.
     * Used for routing and configuration.
     *
     * @return gateway identifier (e.g., "stripe", "paypal", "square")
     */
    String getGatewayId();

    /**
     * Returns the display name for this gateway.
     * Used in admin UI and logs.
     *
     * @return human-readable gateway name (e.g., "Stripe", "PayPal")
     */
    String getGatewayName();

    /**
     * Check if the gateway is properly configured and available.
     *
     * @return true if the gateway is ready to process payments
     */
    boolean isAvailable();

    // ========== Customer Management ==========

    /**
     * Create a customer in the payment gateway.
     * Customers are used to store payment methods and transaction history.
     *
     * @param request customer creation request with email, name, metadata
     * @return GatewayCustomer with the gateway-specific customer ID
     */
    GatewayCustomer createCustomer(CreateCustomerRequest request);

    /**
     * Update an existing customer's details in the gateway.
     *
     * @param gatewayCustomerId the gateway's customer ID
     * @param request updated customer details
     * @return updated GatewayCustomer
     */
    GatewayCustomer updateCustomer(String gatewayCustomerId, UpdateCustomerRequest request);

    /**
     * Retrieve a customer from the gateway.
     *
     * @param gatewayCustomerId the gateway's customer ID
     * @return GatewayCustomer with details
     */
    GatewayCustomer getCustomer(String gatewayCustomerId);

    // ========== Payment Intent (One-Time Payments) ==========

    /**
     * Create a payment intent for processing a payment.
     * Returns a client secret for frontend completion.
     *
     * @param request payment details including amount, currency, customer
     * @return GatewayPaymentIntent with client secret and intent ID
     */
    GatewayPaymentIntent createPaymentIntent(CreatePaymentIntentRequest request);

    /**
     * Confirm a payment intent (typically called from webhook or backend).
     *
     * @param paymentIntentId the gateway's payment intent ID
     * @return GatewayPaymentIntent with updated status
     */
    GatewayPaymentIntent confirmPaymentIntent(String paymentIntentId);

    /**
     * Confirm a payment intent with a specific payment method.
     *
     * @param paymentIntentId the gateway's payment intent ID
     * @param paymentMethodId optional payment method ID to use for confirmation
     * @return GatewayPaymentIntent with updated status
     */
    default GatewayPaymentIntent confirmPaymentIntent(String paymentIntentId, String paymentMethodId) {
        // Default implementation ignores paymentMethodId - override if gateway supports it
        return confirmPaymentIntent(paymentIntentId);
    }

    /**
     * Retrieve a payment intent to check its status.
     *
     * @param paymentIntentId the gateway's payment intent ID
     * @return GatewayPaymentIntent with current status
     */
    GatewayPaymentIntent retrievePaymentIntent(String paymentIntentId);

    /**
     * Cancel a pending payment intent.
     *
     * @param paymentIntentId the gateway's payment intent ID
     * @return GatewayPaymentIntent with canceled status
     */
    GatewayPaymentIntent cancelPaymentIntent(String paymentIntentId);

    // ========== Setup Intent (Save Card Without Charging) ==========

    /**
     * Create a setup intent for saving a payment method without charging.
     * Used when customers want to add a card to their wallet.
     *
     * @param request setup intent details including customer ID
     * @return GatewaySetupIntent with client secret
     */
    GatewaySetupIntent createSetupIntent(CreateSetupIntentRequest request);

    /**
     * Confirm a setup intent (typically after frontend completion).
     *
     * @param setupIntentId the gateway's setup intent ID
     * @return GatewaySetupIntent with the attached payment method ID
     */
    GatewaySetupIntent confirmSetupIntent(String setupIntentId);

    /**
     * Retrieve a setup intent to check its status.
     *
     * @param setupIntentId the gateway's setup intent ID
     * @return GatewaySetupIntent with current status
     */
    GatewaySetupIntent retrieveSetupIntent(String setupIntentId);

    // ========== Payment Method Management ==========

    /**
     * Retrieve details of a saved payment method.
     *
     * @param paymentMethodId the gateway's payment method ID
     * @return GatewayPaymentMethod with card details (masked)
     */
    GatewayPaymentMethod getPaymentMethod(String paymentMethodId);

    /**
     * List all payment methods for a customer.
     *
     * @param gatewayCustomerId the gateway's customer ID
     * @param type payment method type (e.g., "card", "bank_account")
     * @return list of GatewayPaymentMethod objects
     */
    List<GatewayPaymentMethod> listPaymentMethods(String gatewayCustomerId, String type);

    /**
     * Attach a payment method to a customer.
     *
     * @param paymentMethodId the gateway's payment method ID
     * @param gatewayCustomerId the gateway's customer ID
     * @return attached GatewayPaymentMethod
     */
    GatewayPaymentMethod attachPaymentMethod(String paymentMethodId, String gatewayCustomerId);

    /**
     * Detach a payment method from a customer (soft delete).
     *
     * @param paymentMethodId the gateway's payment method ID
     */
    void detachPaymentMethod(String paymentMethodId);

    /**
     * Set a payment method as the customer's default.
     *
     * @param gatewayCustomerId the gateway's customer ID
     * @param paymentMethodId the payment method to set as default
     */
    void setDefaultPaymentMethod(String gatewayCustomerId, String paymentMethodId);

    // ========== Refunds ==========

    /**
     * Create a full refund for a payment.
     *
     * @param request refund details including payment intent ID
     * @return GatewayRefund with refund status
     */
    GatewayRefund createRefund(CreateRefundRequest request);

    /**
     * Create a partial refund for a payment.
     *
     * @param request refund details including amount
     * @return GatewayRefund with refund status
     */
    GatewayRefund createPartialRefund(CreateRefundRequest request);

    /**
     * Retrieve a refund to check its status.
     *
     * @param refundId the gateway's refund ID
     * @return GatewayRefund with current status
     */
    GatewayRefund retrieveRefund(String refundId);

    // ========== Webhook Processing ==========

    /**
     * Verify webhook signature to ensure authenticity.
     *
     * @param payload raw webhook payload
     * @param signature signature header from webhook
     * @return true if signature is valid
     */
    boolean verifyWebhookSignature(String payload, String signature);

    /**
     * Parse webhook payload into a standardized event object.
     *
     * @param payload raw webhook payload
     * @param signature signature header for verification
     * @return GatewayWebhookEvent with event type and data
     */
    GatewayWebhookEvent parseWebhookEvent(String payload, String signature);

    // ========== Address Verification ==========

    /**
     * Get address verification (AVS) results for a payment method.
     *
     * @param paymentMethodId the gateway's payment method ID
     * @return AddressVerificationResult with check outcomes
     */
    AddressVerificationResult getAddressVerification(String paymentMethodId);

    // ========== Connected Accounts (Marketplace/Platform) ==========

    /**
     * Create a connected account for a merchant/store.
     *
     * @param request connected account details
     * @return GatewayConnectedAccount with account ID
     */
    GatewayConnectedAccount createConnectedAccount(CreateConnectedAccountRequest request);

    /**
     * Generate onboarding/update link for a connected account.
     *
     * @param accountId the gateway's connected account ID
     * @param type link type (onboarding, update)
     * @param returnUrl URL to redirect after completion
     * @param refreshUrl URL to redirect if link expires
     * @return URL for the merchant to complete onboarding
     */
    String createAccountLink(String accountId, String type, String returnUrl, String refreshUrl);

    /**
     * Retrieve details of a connected account.
     *
     * @param accountId the gateway's connected account ID
     * @return GatewayConnectedAccount with status and capabilities
     */
    GatewayConnectedAccount getConnectedAccount(String accountId);

    /**
     * Create a transfer to a connected account.
     *
     * @param request transfer details
     * @return GatewayTransfer with transfer ID and status
     */
    GatewayTransfer createTransfer(CreateTransferRequest request);

    // ========== Subscriptions ==========

    /**
     * Create a subscription for a customer.
     *
     * @param request subscription creation details (customer ID, price ID, etc.)
     * @return GatewaySubscription with subscription ID and status
     */
    GatewaySubscription createSubscription(CreateSubscriptionRequest request);

    /**
     * Cancel a subscription.
     *
     * @param subscriptionId the gateway's subscription ID
     * @param cancelAtPeriodEnd if true, cancel at end of current period; if false, cancel immediately
     * @return GatewaySubscription with updated status
     */
    GatewaySubscription cancelSubscription(String subscriptionId, boolean cancelAtPeriodEnd);

    /**
     * Retrieve a subscription to check its status.
     *
     * @param subscriptionId the gateway's subscription ID
     * @return GatewaySubscription with current status
     */
    GatewaySubscription retrieveSubscription(String subscriptionId);

    /**
     * Create a Checkout Session for subscription payment.
     * Returns a URL to redirect the customer to Stripe-hosted checkout.
     *
     * @param request checkout session creation details
     * @return GatewayCheckoutSession with session ID and URL
     */
    GatewayCheckoutSession createCheckoutSession(CreateCheckoutSessionRequest request);
}
