package com.vedvix.syncledger.payment.api;

import com.vedvix.syncledger.payment.gateway.PaymentGateway;
import com.vedvix.syncledger.payment.gateway.PaymentGatewayFactory;
import com.vedvix.syncledger.payment.gateway.dto.*;
import com.vedvix.syncledger.payment.gateway.exception.PaymentGatewayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Facade for Subscription Payment operations - PUBLIC API for the Payment Module.
 *
 * <p>This facade exposes subscription billing operations to other modules
 * (particularly the subscription service). Other modules should ONLY use
 * this facade, not internal gateway services directly.
 *
 * <p>Supports:
 * <ul>
 *   <li>Creating Stripe customers for organizations</li>
 *   <li>Creating Checkout Sessions for subscription payments</li>
 *   <li>Creating subscriptions directly (with existing payment method)</li>
 *   <li>Cancelling subscriptions</li>
 *   <li>Retrieving subscription status</li>
 * </ul>
 *
 * @author vedvix
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionPaymentFacade {

    private final PaymentGatewayFactory gatewayFactory;

    // ========== Customer Management ==========

    /**
     * Create a Stripe customer for an organization.
     *
     * @param email           org contact email
     * @param name            org name
     * @param organizationId  internal org ID for metadata
     * @return GatewayCustomer with the Stripe customer ID
     */
    public GatewayCustomer createCustomerForOrganization(String email, String name, Long organizationId) {
        log.info("Creating payment customer for org: {} ({})", name, organizationId);

        PaymentGateway gateway = gatewayFactory.getDefaultGateway();

        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .email(email)
                .name(name)
                .metadata(Map.of(
                        "organization_id", organizationId.toString(),
                        "type", "organization"
                ))
                .build();

        try {
            GatewayCustomer customer = gateway.createCustomer(request);
            log.info("Payment customer created: {} for org {}", customer.getId(), organizationId);
            return customer;
        } catch (PaymentGatewayException e) {
            log.error("Failed to create customer for org {}: {}", organizationId, e.getMessage());
            throw e;
        }
    }

    // ========== Checkout Session ==========

    /**
     * Create a Stripe Checkout Session for subscription payment.
     * Returns a URL to redirect the customer to Stripe-hosted checkout.
     *
     * @param stripeCustomerId  Stripe customer ID
     * @param priceId           Stripe price ID for the plan
     * @param organizationId    internal org ID for metadata
     * @param successUrl        redirect URL after successful payment
     * @param cancelUrl         redirect URL if customer cancels
     * @return GatewayCheckoutSession with session URL
     */
    public GatewayCheckoutSession createCheckoutSession(
            String stripeCustomerId,
            String priceId,
            Long organizationId,
            String successUrl,
            String cancelUrl) {

        log.info("Creating checkout session for org {} with price {}", organizationId, priceId);

        PaymentGateway gateway = gatewayFactory.getDefaultGateway();

        CreateCheckoutSessionRequest request = CreateCheckoutSessionRequest.builder()
                .gatewayCustomerId(stripeCustomerId)
                .priceId(priceId)
                .mode("subscription")
                .successUrl(successUrl)
                .cancelUrl(cancelUrl)
                .metadata(Map.of(
                        "organization_id", organizationId.toString()
                ))
                .build();

        try {
            GatewayCheckoutSession session = gateway.createCheckoutSession(request);
            if (!session.isSuccess()) {
                throw new PaymentGatewayException("Checkout session creation failed: " + session.getErrorMessage());
            }
            log.info("Checkout session created: {} for org {}", session.getSessionId(), organizationId);
            return session;
        } catch (PaymentGatewayException e) {
            log.error("Failed to create checkout session for org {}: {}", organizationId, e.getMessage());
            throw e;
        }
    }

    // ========== Subscription Management ==========

    /**
     * Create a subscription directly (for customers with existing payment methods).
     *
     * @param stripeCustomerId       Stripe customer ID
     * @param priceId                Stripe price ID
     * @param defaultPaymentMethodId saved payment method ID
     * @param organizationId         internal org ID for metadata
     * @return GatewaySubscription with subscription details
     */
    public GatewaySubscription createSubscription(
            String stripeCustomerId,
            String priceId,
            String defaultPaymentMethodId,
            Long organizationId) {

        log.info("Creating subscription for org {} with price {}", organizationId, priceId);

        PaymentGateway gateway = gatewayFactory.getDefaultGateway();

        CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                .gatewayCustomerId(stripeCustomerId)
                .priceId(priceId)
                .defaultPaymentMethodId(defaultPaymentMethodId)
                .metadata(Map.of(
                        "organization_id", organizationId.toString()
                ))
                .build();

        try {
            GatewaySubscription subscription = gateway.createSubscription(request);
            if (!subscription.isSuccess()) {
                throw new PaymentGatewayException("Subscription creation failed: " + subscription.getErrorMessage());
            }
            log.info("Subscription created: {} for org {}", subscription.getSubscriptionId(), organizationId);
            return subscription;
        } catch (PaymentGatewayException e) {
            log.error("Failed to create subscription for org {}: {}", organizationId, e.getMessage());
            throw e;
        }
    }

    /**
     * Cancel a subscription.
     *
     * @param stripeSubscriptionId Stripe subscription ID
     * @param cancelAtPeriodEnd    if true, cancel at end of billing period; if false, cancel immediately
     * @return GatewaySubscription with updated status
     */
    public GatewaySubscription cancelSubscription(String stripeSubscriptionId, boolean cancelAtPeriodEnd) {
        log.info("Cancelling subscription: {} (atPeriodEnd: {})", stripeSubscriptionId, cancelAtPeriodEnd);

        PaymentGateway gateway = gatewayFactory.getDefaultGateway();

        try {
            GatewaySubscription result = gateway.cancelSubscription(stripeSubscriptionId, cancelAtPeriodEnd);
            if (!result.isSuccess()) {
                throw new PaymentGatewayException("Subscription cancellation failed: " + result.getErrorMessage());
            }
            log.info("Subscription {} cancelled", stripeSubscriptionId);
            return result;
        } catch (PaymentGatewayException e) {
            log.error("Failed to cancel subscription {}: {}", stripeSubscriptionId, e.getMessage());
            throw e;
        }
    }

    /**
     * Retrieve current subscription status from the payment gateway.
     *
     * @param stripeSubscriptionId Stripe subscription ID
     * @return GatewaySubscription with current status
     */
    public GatewaySubscription retrieveSubscription(String stripeSubscriptionId) {
        log.debug("Retrieving subscription: {}", stripeSubscriptionId);

        PaymentGateway gateway = gatewayFactory.getDefaultGateway();

        try {
            return gateway.retrieveSubscription(stripeSubscriptionId);
        } catch (PaymentGatewayException e) {
            log.error("Failed to retrieve subscription {}: {}", stripeSubscriptionId, e.getMessage());
            throw e;
        }
    }
}
