package com.vedvix.syncledger.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for the Payment Module.
 * 
 * Usage in application.yml:
 * <pre>
 * sipstr:
 *   payment:
 *     default-gateway: stripe
 *     default-currency: usd
 *     platform-fee-percent: 10.0
 *     max-payment-methods: 5
 *     stripe:
 *       api-key: ${STRIPE_SECRET_KEY}
 *       webhook-secret: ${STRIPE_WEBHOOK_SECRET}
 *       connect-webhook-secret: ${STRIPE_CONNECT_WEBHOOK_SECRET}
 *     paypal:
 *       client-id: ${PAYPAL_CLIENT_ID}
 *       client-secret: ${PAYPAL_CLIENT_SECRET}
 * </pre>
 */
@Configuration
@ConfigurationProperties(prefix = "sipstr.payment")
@Getter
@Setter
public class PaymentModuleProperties {

    /**
     * Default payment gateway to use (e.g., "stripe", "paypal", "square")
     */
    private String defaultGateway = "stripe";

    /**
     * Default currency for payments
     */
    private String defaultCurrency = "usd";

    /**
     * Platform fee percentage for marketplace transactions
     */
    private BigDecimal platformFeePercent = BigDecimal.valueOf(10.0);

    /**
     * Maximum number of saved payment methods per customer
     */
    private int maxPaymentMethods = 5;

    /**
     * Payment intent expiry in minutes
     */
    private int paymentIntentExpiryMinutes = 30;

    /**
     * Enable/disable address verification
     */
    private boolean addressVerificationEnabled = true;

    /**
     * Stripe-specific configuration
     */
    private StripeProperties stripe = new StripeProperties();

    /**
     * PayPal-specific configuration (for future use)
     */
    private PayPalProperties paypal = new PayPalProperties();

    /**
     * Square-specific configuration (for future use)
     */
    private SquareProperties square = new SquareProperties();

    @Getter
    @Setter
    public static class StripeProperties {
        private String apiKey;
        private String publishableKey;
        private String webhookSecret;
        private String connectWebhookSecret;
        private String accountCountry = "US";

        /**
         * Mapping of subscription plan price IDs.
         * Keys follow convention: &lt;plan&gt;_&lt;cycle&gt; (e.g., "starter_monthly", "professional_annual")
         * Values are Stripe Price IDs (e.g., "price_xxx")
         *
         * <pre>
         * sipstr:
         *   payment:
         *     stripe:
         *       prices:
         *         starter_monthly: price_xxx
         *         starter_annual: price_xxx
         *         professional_monthly: price_xxx
         *         professional_annual: price_xxx
         *         enterprise_monthly: price_xxx
         *         enterprise_annual: price_xxx
         * </pre>
         */
        private Map<String, String> prices = new HashMap<>();
    }

    @Getter
    @Setter
    public static class PayPalProperties {
        private String clientId;
        private String clientSecret;
        private String mode = "sandbox"; // sandbox or live
        private String webhookId;
    }

    @Getter
    @Setter
    public static class SquareProperties {
        private String accessToken;
        private String applicationId;
        private String locationId;
        private String environment = "sandbox"; // sandbox or production
    }
}
