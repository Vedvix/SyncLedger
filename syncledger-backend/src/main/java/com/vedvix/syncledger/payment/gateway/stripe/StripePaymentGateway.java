package com.vedvix.syncledger.payment.gateway.stripe;

import com.vedvix.syncledger.payment.gateway.PaymentGateway;
import com.vedvix.syncledger.payment.gateway.dto.*;
import com.vedvix.syncledger.payment.gateway.exception.PaymentGatewayException;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Stripe implementation of the PaymentGateway interface.
 * This is the primary (default) payment gateway.
 * 
 * <p>To switch to a different gateway, either:
 * <ul>
 *   <li>Remove @Primary from this class</li>
 *   <li>Use @ConditionalOnProperty for gateway selection</li>
 *   <li>Use PaymentGatewayFactory for dynamic selection</li>
 * </ul>
 */
@Slf4j
@Service
@Primary
public class StripePaymentGateway implements PaymentGateway {

    private static final String GATEWAY_ID = "stripe";
    private static final String GATEWAY_NAME = "Stripe";
    
    // Avoid instance method writing static field
    private static final AtomicBoolean STRIPE_INITIALIZED = new AtomicBoolean(false);

    @Value("${sipstr.payment.stripe.api-key:}")
    private String stripeSecretKey;

    @Value("${sipstr.payment.stripe.webhook-secret:}")
    private String webhookSecret;

    @Value("${sipstr.payment.stripe.webhook-tolerance-seconds:300}")
    private long webhookTolerance;

    @PostConstruct
    public void init() {
        initializeStripeOnce(stripeSecretKey);
        log.info("Stripe Payment Gateway initialized");
    }

    private static synchronized void initializeStripeOnce(String apiKey) {
        if (STRIPE_INITIALIZED.compareAndSet(false, true)) {
            Stripe.apiKey = apiKey;
            log.info("Stripe API key configured");
        }
    }

    // ========== Gateway Identification ==========

    @Override
    public String getGatewayId() {
        return GATEWAY_ID;
    }

    @Override
    public String getGatewayName() {
        return GATEWAY_NAME;
    }

    @Override
    public boolean isAvailable() {
        return stripeSecretKey != null && !stripeSecretKey.isBlank();
    }

    // ========== Customer Management ==========

    @Override
    public GatewayCustomer createCustomer(CreateCustomerRequest request) {
        log.info("Creating Stripe customer for user: {}", request.getUserUuid());
        
        try {
            CustomerCreateParams.Builder builder = CustomerCreateParams.builder()
                    .setEmail(request.getEmail())
                    .setName(request.getName())
                    .setPhone(request.getPhone());
            
            // Add metadata
            if (request.getMetadata() != null) {
                request.getMetadata().forEach(builder::putMetadata);
            }
            if (request.getUserUuid() != null) {
                builder.putMetadata("sipstr_user_uuid", request.getUserUuid());
            }
            if (request.getUserId() != null) {
                builder.putMetadata("sipstr_user_id", request.getUserId().toString());
            }

            String idempotencyKey = "customer_" + request.getUserUuid();
            RequestOptions options = RequestOptions.builder()
                    .setIdempotencyKey(idempotencyKey)
                    .build();

            Customer customer = Customer.create(builder.build(), options);
            
            log.info("Created Stripe customer: {}", customer.getId());

            return GatewayCustomer.builder()
                    .gatewayCustomerId(customer.getId())
                    .email(customer.getEmail())
                    .name(customer.getName())
                    .phone(customer.getPhone())
                    .createdAt(customer.getCreated())
                    .build();

        } catch (StripeException e) {
            log.error("Failed to create Stripe customer: {}", e.getMessage(), e);
            throw new PaymentGatewayException("Failed to create customer", e);
        }
    }

    @Override
    public GatewayCustomer updateCustomer(String gatewayCustomerId, UpdateCustomerRequest request) {
        log.info("Updating Stripe customer: {}", gatewayCustomerId);
        
        try {
            Customer customer = Customer.retrieve(gatewayCustomerId);
            
            CustomerUpdateParams.Builder builder = CustomerUpdateParams.builder();
            
            if (request.getEmail() != null) builder.setEmail(request.getEmail());
            if (request.getName() != null) builder.setName(request.getName());
            if (request.getPhone() != null) builder.setPhone(request.getPhone());
            if (request.getMetadata() != null) {
                request.getMetadata().forEach(builder::putMetadata);
            }

            Customer updated = customer.update(builder.build());

            return GatewayCustomer.builder()
                    .gatewayCustomerId(updated.getId())
                    .email(updated.getEmail())
                    .name(updated.getName())
                    .phone(updated.getPhone())
                    .createdAt(updated.getCreated())
                    .build();

        } catch (StripeException e) {
            log.error("Failed to update Stripe customer: {}", e.getMessage(), e);
            throw new PaymentGatewayException("Failed to update customer", e);
        }
    }

    @Override
    public GatewayCustomer getCustomer(String gatewayCustomerId) {
        try {
            Customer customer = Customer.retrieve(gatewayCustomerId);
            
            String defaultPmId = null;
            if (customer.getInvoiceSettings() != null) {
                defaultPmId = customer.getInvoiceSettings().getDefaultPaymentMethod();
            }

            return GatewayCustomer.builder()
                    .gatewayCustomerId(customer.getId())
                    .email(customer.getEmail())
                    .name(customer.getName())
                    .phone(customer.getPhone())
                    .defaultPaymentMethodId(defaultPmId)
                    .createdAt(customer.getCreated())
                    .build();

        } catch (StripeException e) {
            log.error("Failed to retrieve Stripe customer: {}", e.getMessage(), e);
            throw new PaymentGatewayException("Failed to retrieve customer", e);
        }
    }

    // ========== Payment Intent ==========

    @Override
    public GatewayPaymentIntent createPaymentIntent(CreatePaymentIntentRequest request) {
        log.info("Creating Stripe PaymentIntent: amount={}, currency={}, autoConfirmTestMode={}", 
                request.getAmountInCents(), request.getCurrency(), request.isAutoConfirmTestMode());
        
        try {
            PaymentIntentCreateParams.Builder builder = PaymentIntentCreateParams.builder()
                    .setAmount(request.getAmountInCentsResolved())
                    .setCurrency(request.getCurrency());

            // ========================================
            // TEST MODE: Auto-confirm for backend testing
            // ========================================
            if (request.isAutoConfirmTestMode()) {
                String testMethod = request.getTestPaymentMethod() != null 
                        ? request.getTestPaymentMethod() 
                        : "pm_card_visa";
                
                log.warn("AUTO-CONFIRM TEST MODE - Using test payment method: {}", testMethod);
                
                builder.addPaymentMethodType("card")
                        .setPaymentMethod(testMethod)
                        .setConfirm(true)
                        .putMetadata("auto_confirmed", "true")
                        .putMetadata("test_mode", "true");
                        
            } else {
                // ========================================
                // PRODUCTION MODE: Frontend completes payment
                // ========================================
                builder.setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                );

                if (request.getGatewayCustomerId() != null) {
                    builder.setCustomer(request.getGatewayCustomerId());
                }

                if (request.getPaymentMethodId() != null) {
                    builder.setPaymentMethod(request.getPaymentMethodId());
                    
                    if (request.isConfirmImmediately()) {
                        builder.setConfirm(true);
                    }
                    if (request.isOffSession()) {
                        builder.setOffSession(true);
                    }
                }
            }

            if (request.getDescription() != null) {
                builder.setDescription(request.getDescription());
            }

            // Add metadata
            if (request.getMetadata() != null) {
                request.getMetadata().forEach(builder::putMetadata);
            }
            if (request.getOrderId() != null) {
                builder.putMetadata("order_id", request.getOrderId());
            }

            String idempotencyKey = UUID.randomUUID().toString();
            RequestOptions options = RequestOptions.builder()
                    .setIdempotencyKey(idempotencyKey)
                    .build();

            PaymentIntent intent = PaymentIntent.create(builder.build(), options);

            log.info("Created Stripe PaymentIntent: {} with status: {}", intent.getId(), intent.getStatus());

            return GatewayPaymentIntent.builder()
                    .paymentIntentId(intent.getId())
                    .clientSecret(intent.getClientSecret())
                    .status(intent.getStatus())
                    .amount(intent.getAmount())
                    .currency(intent.getCurrency())
                    .customerId(intent.getCustomer())
                    .paymentMethodId(intent.getPaymentMethod())
                    .autoConfirmed(request.isAutoConfirmTestMode())
                    .success(true)
                    .build();

        } catch (StripeException e) {
            log.error("Failed to create PaymentIntent: {}", e.getMessage(), e);
            return GatewayPaymentIntent.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public GatewayPaymentIntent confirmPaymentIntent(String paymentIntentId) {
        return confirmPaymentIntent(paymentIntentId, null);
    }

    @Override
    public GatewayPaymentIntent confirmPaymentIntent(String paymentIntentId, String paymentMethodId) {
        log.info("Confirming Stripe PaymentIntent: {} with method: {}", paymentIntentId, paymentMethodId);
        
        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            
            PaymentIntentConfirmParams.Builder confirmBuilder = PaymentIntentConfirmParams.builder();
            if (paymentMethodId != null && !paymentMethodId.isBlank()) {
                confirmBuilder.setPaymentMethod(paymentMethodId);
            }
            
            PaymentIntent confirmed = intent.confirm(confirmBuilder.build());

            return GatewayPaymentIntent.builder()
                    .paymentIntentId(confirmed.getId())
                    .clientSecret(confirmed.getClientSecret())
                    .status(confirmed.getStatus())
                    .amount(confirmed.getAmount())
                    .currency(confirmed.getCurrency())
                    .success(true)
                    .build();

        } catch (StripeException e) {
            log.error("Failed to confirm PaymentIntent: {}", e.getMessage(), e);
            return GatewayPaymentIntent.builder()
                    .paymentIntentId(paymentIntentId)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public GatewayPaymentIntent retrievePaymentIntent(String paymentIntentId) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);

            String failureMessage = null;
            String failureCode = null;
            if (intent.getLastPaymentError() != null) {
                failureMessage = intent.getLastPaymentError().getMessage();
                failureCode = intent.getLastPaymentError().getCode();
            }

            return GatewayPaymentIntent.builder()
                    .paymentIntentId(intent.getId())
                    .clientSecret(intent.getClientSecret())
                    .status(intent.getStatus())
                    .amount(intent.getAmount())
                    .currency(intent.getCurrency())
                    .customerId(intent.getCustomer())
                    .paymentMethodId(intent.getPaymentMethod())
                    .failureMessage(failureMessage)
                    .failureCode(failureCode)
                    .success(true)
                    .build();

        } catch (StripeException e) {
            log.error("Failed to retrieve PaymentIntent: {}", e.getMessage(), e);
            throw new PaymentGatewayException("Failed to retrieve payment intent", e);
        }
    }

    @Override
    public GatewayPaymentIntent cancelPaymentIntent(String paymentIntentId) {
        log.info("Canceling Stripe PaymentIntent: {}", paymentIntentId);
        
        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            PaymentIntent canceled = intent.cancel();

            return GatewayPaymentIntent.builder()
                    .paymentIntentId(canceled.getId())
                    .status(canceled.getStatus())
                    .success(true)
                    .build();

        } catch (StripeException e) {
            log.error("Failed to cancel PaymentIntent: {}", e.getMessage(), e);
            throw new PaymentGatewayException("Failed to cancel payment intent", e);
        }
    }

    // ========== Setup Intent ==========

    @Override
    public GatewaySetupIntent createSetupIntent(CreateSetupIntentRequest request) {
        log.info("Creating Stripe SetupIntent for customer: {}", request.getGatewayCustomerId());
        
        try {
            SetupIntentCreateParams.Builder builder = SetupIntentCreateParams.builder()
                    .setCustomer(request.getGatewayCustomerId())
                    .addPaymentMethodType("card")
                    .setUsage(SetupIntentCreateParams.Usage.OFF_SESSION);

            if (request.getMetadata() != null) {
                request.getMetadata().forEach(builder::putMetadata);
            }

            String idempotencyKey = "setup_" + request.getGatewayCustomerId() + "_" + System.currentTimeMillis();
            RequestOptions options = RequestOptions.builder()
                    .setIdempotencyKey(idempotencyKey)
                    .build();

            SetupIntent setupIntent = SetupIntent.create(builder.build(), options);

            log.info("Created Stripe SetupIntent: {}", setupIntent.getId());

            return GatewaySetupIntent.builder()
                    .setupIntentId(setupIntent.getId())
                    .clientSecret(setupIntent.getClientSecret())
                    .status(setupIntent.getStatus())
                    .customerId(setupIntent.getCustomer())
                    .success(true)
                    .build();

        } catch (StripeException e) {
            log.error("Failed to create SetupIntent: {}", e.getMessage(), e);
            return GatewaySetupIntent.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public GatewaySetupIntent confirmSetupIntent(String setupIntentId) {
        log.info("Confirming Stripe SetupIntent: {}", setupIntentId);
        
        try {
            SetupIntent intent = SetupIntent.retrieve(setupIntentId);
            SetupIntent confirmed = intent.confirm();

            return GatewaySetupIntent.builder()
                    .setupIntentId(confirmed.getId())
                    .clientSecret(confirmed.getClientSecret())
                    .status(confirmed.getStatus())
                    .customerId(confirmed.getCustomer())
                    .paymentMethodId(confirmed.getPaymentMethod())
                    .success(true)
                    .build();

        } catch (StripeException e) {
            log.error("Failed to confirm SetupIntent: {}", e.getMessage(), e);
            return GatewaySetupIntent.builder()
                    .setupIntentId(setupIntentId)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public GatewaySetupIntent retrieveSetupIntent(String setupIntentId) {
        try {
            SetupIntent intent = SetupIntent.retrieve(setupIntentId);

            return GatewaySetupIntent.builder()
                    .setupIntentId(intent.getId())
                    .clientSecret(intent.getClientSecret())
                    .status(intent.getStatus())
                    .customerId(intent.getCustomer())
                    .paymentMethodId(intent.getPaymentMethod())
                    .success(true)
                    .build();

        } catch (StripeException e) {
            log.error("Failed to retrieve SetupIntent: {}", e.getMessage(), e);
            throw new PaymentGatewayException("Failed to retrieve setup intent", e);
        }
    }

    // ========== Payment Method Management ==========

    @Override
    public GatewayPaymentMethod getPaymentMethod(String paymentMethodId) {
        try {
            PaymentMethod pm = PaymentMethod.retrieve(paymentMethodId);
            return mapToGatewayPaymentMethod(pm);

        } catch (StripeException e) {
            log.error("Failed to retrieve PaymentMethod: {}", e.getMessage(), e);
            throw new PaymentGatewayException("Failed to retrieve payment method", e);
        }
    }

    @Override
    public List<GatewayPaymentMethod> listPaymentMethods(String gatewayCustomerId, String type) {
        try {
            PaymentMethodListParams.Type pmType = "card".equals(type) 
                    ? PaymentMethodListParams.Type.CARD 
                    : PaymentMethodListParams.Type.CARD;

            PaymentMethodListParams params = PaymentMethodListParams.builder()
                    .setCustomer(gatewayCustomerId)
                    .setType(pmType)
                    .setLimit(20L)
                    .build();

            PaymentMethodCollection methods = PaymentMethod.list(params);
            
            return methods.getData().stream()
                    .map(this::mapToGatewayPaymentMethod)
                    .collect(Collectors.toList());

        } catch (StripeException e) {
            log.error("Failed to list PaymentMethods: {}", e.getMessage(), e);
            throw new PaymentGatewayException("Failed to list payment methods", e);
        }
    }

    @Override
    public GatewayPaymentMethod attachPaymentMethod(String paymentMethodId, String gatewayCustomerId) {
        log.info("Attaching PaymentMethod {} to customer {}", paymentMethodId, gatewayCustomerId);
        
        try {
            PaymentMethod pm = PaymentMethod.retrieve(paymentMethodId);
            
            PaymentMethodAttachParams params = PaymentMethodAttachParams.builder()
                    .setCustomer(gatewayCustomerId)
                    .build();

            PaymentMethod attached = pm.attach(params);
            
            return mapToGatewayPaymentMethod(attached);

        } catch (StripeException e) {
            log.error("Failed to attach PaymentMethod: {}", e.getMessage(), e);
            throw new PaymentGatewayException("Failed to attach payment method", e);
        }
    }

    @Override
    public void detachPaymentMethod(String paymentMethodId) {
        log.info("Detaching PaymentMethod: {}", paymentMethodId);
        
        try {
            PaymentMethod pm = PaymentMethod.retrieve(paymentMethodId);
            pm.detach();
            log.info("Successfully detached PaymentMethod: {}", paymentMethodId);

        } catch (StripeException e) {
            log.error("Failed to detach PaymentMethod: {}", e.getMessage(), e);
            throw new PaymentGatewayException("Failed to detach payment method", e);
        }
    }

    @Override
    public void setDefaultPaymentMethod(String gatewayCustomerId, String paymentMethodId) {
        log.info("Setting default PaymentMethod {} for customer {}", paymentMethodId, gatewayCustomerId);
        
        try {
            Customer customer = Customer.retrieve(gatewayCustomerId);

            CustomerUpdateParams params = CustomerUpdateParams.builder()
                    .setInvoiceSettings(
                            CustomerUpdateParams.InvoiceSettings.builder()
                                    .setDefaultPaymentMethod(paymentMethodId)
                                    .build()
                    )
                    .build();

            customer.update(params);
            log.info("Successfully set default PaymentMethod for customer: {}", gatewayCustomerId);

        } catch (StripeException e) {
            log.error("Failed to set default PaymentMethod: {}", e.getMessage(), e);
            throw new PaymentGatewayException("Failed to set default payment method", e);
        }
    }

    // ========== Refunds ==========

    @Override
    public GatewayRefund createRefund(CreateRefundRequest request) {
        log.info("Creating Stripe refund for PaymentIntent: {}", request.getPaymentIntentId());
        
        try {
            RefundCreateParams.Builder builder = RefundCreateParams.builder()
                    .setPaymentIntent(request.getPaymentIntentId());

            if (request.getReason() != null) {
                builder.setReason(mapRefundReason(request.getReasonCode()));
            }
            if (request.getMetadata() != null) {
                request.getMetadata().forEach(builder::putMetadata);
            }

            Refund refund = Refund.create(builder.build());

            log.info("Created Stripe refund: {}", refund.getId());

            return GatewayRefund.builder()
                    .refundId(refund.getId())
                    .paymentIntentId(refund.getPaymentIntent())
                    .amount(refund.getAmount())
                    .currency(refund.getCurrency())
                    .status(refund.getStatus())
                    .reason(refund.getReason())
                    .balanceTransactionId(refund.getBalanceTransaction())
                    .createdAt(refund.getCreated())
                    .success(true)
                    .build();

        } catch (StripeException e) {
            log.error("Failed to create refund: {}", e.getMessage(), e);
            String errorMsg = e.getMessage();
            // Provide a clear message for already-refunded charges
            if (e.getCode() != null && e.getCode().equals("charge_already_refunded")) {
                errorMsg = "This charge has already been fully refunded in Stripe. No further refund is possible.";
            }
            return GatewayRefund.builder()
                    .success(false)
                    .errorMessage(errorMsg)
                    .build();
        }
    }

    @Override
    public GatewayRefund createPartialRefund(CreateRefundRequest request) {
        log.info("Creating partial Stripe refund: {} cents", request.getAmountInCents());
        
        try {
            RefundCreateParams.Builder builder = RefundCreateParams.builder()
                    .setPaymentIntent(request.getPaymentIntentId())
                    .setAmount(request.getAmountInCents());

            if (request.getReason() != null) {
                builder.setReason(mapRefundReason(request.getReasonCode()));
            }
            if (request.getMetadata() != null) {
                request.getMetadata().forEach(builder::putMetadata);
            }

            Refund refund = Refund.create(builder.build());

            log.info("Created partial Stripe refund: {}", refund.getId());

            return GatewayRefund.builder()
                    .refundId(refund.getId())
                    .paymentIntentId(refund.getPaymentIntent())
                    .amount(refund.getAmount())
                    .currency(refund.getCurrency())
                    .status(refund.getStatus())
                    .balanceTransactionId(refund.getBalanceTransaction())
                    .createdAt(refund.getCreated())
                    .success(true)
                    .build();

        } catch (StripeException e) {
            log.error("Failed to create partial refund: {}", e.getMessage(), e);
            return GatewayRefund.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public GatewayRefund retrieveRefund(String refundId) {
        try {
            Refund refund = Refund.retrieve(refundId);

            return GatewayRefund.builder()
                    .refundId(refund.getId())
                    .paymentIntentId(refund.getPaymentIntent())
                    .amount(refund.getAmount())
                    .currency(refund.getCurrency())
                    .status(refund.getStatus())
                    .reason(refund.getReason())
                    .failureReason(refund.getFailureReason())
                    .balanceTransactionId(refund.getBalanceTransaction())
                    .createdAt(refund.getCreated())
                    .success(true)
                    .build();

        } catch (StripeException e) {
            log.error("Failed to retrieve refund: {}", e.getMessage(), e);
            throw new PaymentGatewayException("Failed to retrieve refund", e);
        }
    }

    // ========== Webhook Processing ==========

    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            Webhook.constructEvent(payload, signature, webhookSecret, webhookTolerance);
            return true;
        } catch (SignatureVerificationException e) {
            log.warn("Webhook signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public GatewayWebhookEvent parseWebhookEvent(String payload, String signature) {
        try {
            Event event = Webhook.constructEvent(payload, signature, webhookSecret, webhookTolerance);
            
            return GatewayWebhookEvent.builder()
                    .eventId(event.getId())
                    .eventType(event.getType())
                    .normalizedType(mapToNormalizedEventType(event.getType()))
                    .rawEvent(event)
                    .createdAt(event.getCreated())
                    .verified(true)
                    .build();

        } catch (SignatureVerificationException e) {
            log.error("Webhook verification failed: {}", e.getMessage());
            return GatewayWebhookEvent.builder()
                    .verified(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    // ========== Address Verification ==========

    @Override
    public AddressVerificationResult getAddressVerification(String paymentMethodId) {
        try {
            PaymentMethod pm = PaymentMethod.retrieve(paymentMethodId);

            String addressCheck = null;
            String postalCheck = null;
            String cvcCheck = null;

            if (pm.getCard() != null && pm.getCard().getChecks() != null) {
                PaymentMethod.Card.Checks checks = pm.getCard().getChecks();
                addressCheck = checks.getAddressLine1Check();
                postalCheck = checks.getAddressPostalCodeCheck();
                cvcCheck = checks.getCvcCheck();
            }

            return AddressVerificationResult.evaluate(paymentMethodId, addressCheck, postalCheck, cvcCheck);

        } catch (StripeException e) {
            log.error("Failed to get AVS results: {}", e.getMessage(), e);
            throw new PaymentGatewayException("Failed to get address verification", e);
        }
    }

    // ========== Connected Accounts ==========

    @Override
    public GatewayConnectedAccount createConnectedAccount(CreateConnectedAccountRequest request) {
        log.info("Creating Stripe connected account for store: {}", request.getStoreUuid());
        
        try {
            AccountCreateParams.Builder builder = AccountCreateParams.builder()
                    .setType(AccountCreateParams.Type.EXPRESS)
                    .setCountry(request.getCountry())
                    .setEmail(request.getEmail())
                    .setBusinessType(mapBusinessType(request.getBusinessType()));

            if (request.getBusinessName() != null) {
                builder.setCompany(AccountCreateParams.Company.builder()
                        .setName(request.getBusinessName())
                        .setPhone(request.getBusinessPhone())
                        .setTaxId(request.getTaxId())
                        .build());
            }

            builder.setCapabilities(AccountCreateParams.Capabilities.builder()
                    .setCardPayments(AccountCreateParams.Capabilities.CardPayments.builder()
                            .setRequested(true).build())
                    .setTransfers(AccountCreateParams.Capabilities.Transfers.builder()
                            .setRequested(true).build())
                    .build());

            if (request.getStoreUuid() != null) {
                builder.putMetadata("sipstr_store_uuid", request.getStoreUuid());
            }

            String idempotencyKey = "account_" + request.getStoreUuid();
            RequestOptions options = RequestOptions.builder()
                    .setIdempotencyKey(idempotencyKey)
                    .build();

            Account account = Account.create(builder.build(), options);

            log.info("Created Stripe connected account: {}", account.getId());

            return GatewayConnectedAccount.builder()
                    .accountId(account.getId())
                    .accountType(account.getType())
                    .email(account.getEmail())
                    .country(account.getCountry())
                    .chargesEnabled(Boolean.TRUE.equals(account.getChargesEnabled()))
                    .payoutsEnabled(Boolean.TRUE.equals(account.getPayoutsEnabled()))
                    .detailsSubmitted(Boolean.TRUE.equals(account.getDetailsSubmitted()))
                    .success(true)
                    .build();

        } catch (StripeException e) {
            log.error("Failed to create connected account: {}", e.getMessage(), e);
            return GatewayConnectedAccount.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public String createAccountLink(String accountId, String type, String returnUrl, String refreshUrl) {
        log.info("Creating account link for: {}", accountId);
        
        try {
            AccountLinkCreateParams.Type linkType = "update".equals(type)
                    ? AccountLinkCreateParams.Type.ACCOUNT_UPDATE
                    : AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING;

            AccountLink link = AccountLink.create(AccountLinkCreateParams.builder()
                    .setAccount(accountId)
                    .setType(linkType)
                    .setReturnUrl(returnUrl)
                    .setRefreshUrl(refreshUrl)
                    .build());

            return link.getUrl();

        } catch (StripeException e) {
            log.error("Failed to create account link: {}", e.getMessage(), e);
            throw new PaymentGatewayException("Failed to create account link", e);
        }
    }

    @Override
    public GatewayConnectedAccount getConnectedAccount(String accountId) {
        try {
            Account account = Account.retrieve(accountId);

            GatewayConnectedAccount.ConnectedBankAccount bankAccount = null;
            if (account.getExternalAccounts() != null && account.getExternalAccounts().getData() != null) {
                bankAccount = account.getExternalAccounts().getData().stream()
                        .filter(BankAccount.class::isInstance)
                        .map(BankAccount.class::cast)
                        .findFirst()
                        .map(ba -> GatewayConnectedAccount.ConnectedBankAccount.builder()
                                .bankName(ba.getBankName())
                                .accountHolderName(ba.getAccountHolderName())
                                .accountHolderType(ba.getAccountHolderType())
                                .last4(ba.getLast4())
                                .currency(ba.getCurrency())
                                .build())
                        .orElse(null);
            }

            List<String> currentlyDue = account.getRequirements() != null 
                    ? account.getRequirements().getCurrentlyDue() 
                    : List.of();
            List<String> pastDue = account.getRequirements() != null 
                    ? account.getRequirements().getPastDue() 
                    : List.of();

            return GatewayConnectedAccount.builder()
                    .accountId(account.getId())
                    .accountType(account.getType())
                    .email(account.getEmail())
                    .businessType(account.getBusinessType())
                    .country(account.getCountry())
                    .chargesEnabled(Boolean.TRUE.equals(account.getChargesEnabled()))
                    .payoutsEnabled(Boolean.TRUE.equals(account.getPayoutsEnabled()))
                    .detailsSubmitted(Boolean.TRUE.equals(account.getDetailsSubmitted()))
                    .currentlyDue(currentlyDue)
                    .pastDue(pastDue)
                    .bankAccount(bankAccount)
                    .success(true)
                    .build();

        } catch (StripeException e) {
            log.error("Failed to retrieve connected account: {}", e.getMessage(), e);
            throw new PaymentGatewayException("Failed to retrieve connected account", e);
        }
    }

    @Override
    public GatewayTransfer createTransfer(CreateTransferRequest request) {
        log.info("Creating Stripe transfer to account: {}", request.getDestinationAccountId());
        
        try {
            TransferCreateParams params = TransferCreateParams.builder()
                    .setAmount(request.getAmountInCents())
                    .setCurrency(request.getCurrency())
                    .setDestination(request.getDestinationAccountId())
                    .setDescription(request.getDescription())
                    .build();

            String idempotencyKey = request.getIdempotencyKey() != null 
                    ? request.getIdempotencyKey()
                    : "transfer_" + UUID.randomUUID();
            
            RequestOptions options = RequestOptions.builder()
                    .setIdempotencyKey(idempotencyKey)
                    .build();

            Transfer transfer = Transfer.create(params, options);

            log.info("Created Stripe transfer: {}", transfer.getId());

            return GatewayTransfer.builder()
                    .transferId(transfer.getId())
                    .destinationAccountId(transfer.getDestination())
                    .amount(transfer.getAmount())
                    .currency(transfer.getCurrency())
                    .balanceTransactionId(transfer.getBalanceTransaction())
                    .createdAt(transfer.getCreated())
                    .success(true)
                    .build();

        } catch (StripeException e) {
            log.error("Failed to create transfer: {}", e.getMessage(), e);
            return GatewayTransfer.failure(e.getMessage());
        }
    }

    // ========== Private Helper Methods ==========

    private GatewayPaymentMethod mapToGatewayPaymentMethod(PaymentMethod pm) {
        GatewayPaymentMethod.GatewayPaymentMethodBuilder builder = GatewayPaymentMethod.builder()
                .paymentMethodId(pm.getId())
                .type(pm.getType())
                .customerId(pm.getCustomer())
                .createdAt(pm.getCreated())
                .success(true);

        if (pm.getCard() != null) {
            PaymentMethod.Card card = pm.getCard();
            builder.brand(card.getBrand())
                    .last4(card.getLast4())
                    .expMonth(card.getExpMonth().intValue())
                    .expYear(card.getExpYear().intValue())
                    .fingerprint(card.getFingerprint())
                    .funding(card.getFunding())
                    .country(card.getCountry());

            if (card.getChecks() != null) {
                builder.addressLine1Check(card.getChecks().getAddressLine1Check())
                        .postalCodeCheck(card.getChecks().getAddressPostalCodeCheck())
                        .cvcCheck(card.getChecks().getCvcCheck());
            }
        }

        if (pm.getBillingDetails() != null) {
            PaymentMethod.BillingDetails billing = pm.getBillingDetails();
            builder.cardholderName(billing.getName());

            if (billing.getAddress() != null) {
                builder.billingAddress(GatewayBillingAddress.builder()
                        .name(billing.getName())
                        .line1(billing.getAddress().getLine1())
                        .line2(billing.getAddress().getLine2())
                        .city(billing.getAddress().getCity())
                        .state(billing.getAddress().getState())
                        .postalCode(billing.getAddress().getPostalCode())
                        .country(billing.getAddress().getCountry())
                        .phone(billing.getPhone())
                        .email(billing.getEmail())
                        .build());
            }
        }

        return builder.build();
    }

    private RefundCreateParams.Reason mapRefundReason(String reasonCode) {
        if (reasonCode == null) return null;
        return switch (reasonCode.toLowerCase()) {
            case "duplicate" -> RefundCreateParams.Reason.DUPLICATE;
            case "fraudulent" -> RefundCreateParams.Reason.FRAUDULENT;
            default -> RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER;
        };
    }

    private AccountCreateParams.BusinessType mapBusinessType(String businessType) {
        if (businessType == null) return AccountCreateParams.BusinessType.COMPANY;
        return switch (businessType.toLowerCase()) {
            case "individual" -> AccountCreateParams.BusinessType.INDIVIDUAL;
            case "non_profit" -> AccountCreateParams.BusinessType.NON_PROFIT;
            case "government_entity" -> AccountCreateParams.BusinessType.GOVERNMENT_ENTITY;
            default -> AccountCreateParams.BusinessType.COMPANY;
        };
    }

    // ========== Subscription Management ==========

    @Override
    public GatewaySubscription createSubscription(CreateSubscriptionRequest request) {
        log.info("Creating Stripe subscription for customer: {}", request.getGatewayCustomerId());

        try {
            SubscriptionCreateParams.Builder builder = SubscriptionCreateParams.builder()
                    .setCustomer(request.getGatewayCustomerId())
                    .addItem(SubscriptionCreateParams.Item.builder()
                            .setPrice(request.getPriceId())
                            .build());

            if (request.getDefaultPaymentMethodId() != null) {
                builder.setDefaultPaymentMethod(request.getDefaultPaymentMethodId());
            }

            if (request.getTrialDays() != null && request.getTrialDays() > 0) {
                builder.setTrialPeriodDays((long) request.getTrialDays());
            }

            builder.setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE);
            builder.addExpand("latest_invoice.payment_intent");

            if (request.getMetadata() != null) {
                request.getMetadata().forEach(builder::putMetadata);
            }

            com.stripe.model.Subscription subscription =
                    com.stripe.model.Subscription.create(builder.build());

            log.info("Created Stripe subscription: {} (status: {})", subscription.getId(), subscription.getStatus());

            return mapToGatewaySubscription(subscription);

        } catch (StripeException e) {
            log.error("Failed to create Stripe subscription: {}", e.getMessage(), e);
            return GatewaySubscription.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public GatewaySubscription cancelSubscription(String subscriptionId, boolean cancelAtPeriodEnd) {
        log.info("Cancelling Stripe subscription: {} (atPeriodEnd: {})", subscriptionId, cancelAtPeriodEnd);

        try {
            com.stripe.model.Subscription subscription =
                    com.stripe.model.Subscription.retrieve(subscriptionId);

            if (cancelAtPeriodEnd) {
                SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                        .setCancelAtPeriodEnd(true)
                        .build();
                subscription = subscription.update(params);
            } else {
                subscription = subscription.cancel();
            }

            log.info("Stripe subscription {} cancelled (status: {})", subscriptionId, subscription.getStatus());

            return mapToGatewaySubscription(subscription);

        } catch (StripeException e) {
            log.error("Failed to cancel Stripe subscription {}: {}", subscriptionId, e.getMessage(), e);
            return GatewaySubscription.builder()
                    .subscriptionId(subscriptionId)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public GatewaySubscription retrieveSubscription(String subscriptionId) {
        log.debug("Retrieving Stripe subscription: {}", subscriptionId);

        try {
            com.stripe.model.Subscription subscription =
                    com.stripe.model.Subscription.retrieve(subscriptionId);

            return mapToGatewaySubscription(subscription);

        } catch (StripeException e) {
            log.error("Failed to retrieve Stripe subscription {}: {}", subscriptionId, e.getMessage(), e);
            return GatewaySubscription.builder()
                    .subscriptionId(subscriptionId)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public GatewayCheckoutSession createCheckoutSession(CreateCheckoutSessionRequest request) {
        log.info("Creating Stripe Checkout Session for customer: {}", request.getGatewayCustomerId());

        try {
            com.stripe.param.checkout.SessionCreateParams.Builder builder =
                    com.stripe.param.checkout.SessionCreateParams.builder()
                            .setMode(com.stripe.param.checkout.SessionCreateParams.Mode.SUBSCRIPTION)
                            .setCustomer(request.getGatewayCustomerId())
                            .setSuccessUrl(request.getSuccessUrl())
                            .setCancelUrl(request.getCancelUrl())
                            .addLineItem(
                                    com.stripe.param.checkout.SessionCreateParams.LineItem.builder()
                                            .setPrice(request.getPriceId())
                                            .setQuantity(1L)
                                            .build()
                            );

            if (request.getTrialDays() != null && request.getTrialDays() > 0) {
                builder.setSubscriptionData(
                        com.stripe.param.checkout.SessionCreateParams.SubscriptionData.builder()
                                .setTrialPeriodDays((long) request.getTrialDays())
                                .build()
                );
            }

            if (request.getMetadata() != null) {
                request.getMetadata().forEach(builder::putMetadata);
            }

            com.stripe.model.checkout.Session session =
                    com.stripe.model.checkout.Session.create(builder.build());

            log.info("Created Stripe Checkout Session: {}", session.getId());

            return GatewayCheckoutSession.builder()
                    .sessionId(session.getId())
                    .url(session.getUrl())
                    .status(session.getStatus())
                    .success(true)
                    .build();

        } catch (StripeException e) {
            log.error("Failed to create Stripe Checkout Session: {}", e.getMessage(), e);
            return GatewayCheckoutSession.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    private GatewaySubscription mapToGatewaySubscription(com.stripe.model.Subscription subscription) {
        return GatewaySubscription.builder()
                .subscriptionId(subscription.getId())
                .customerId(subscription.getCustomer())
                .status(subscription.getStatus())
                .currentPeriodStart(subscription.getCurrentPeriodStart())
                .currentPeriodEnd(subscription.getCurrentPeriodEnd())
                .cancelAtPeriodEnd(Boolean.TRUE.equals(subscription.getCancelAtPeriodEnd()))
                .latestInvoiceId(subscription.getLatestInvoice())
                .defaultPaymentMethodId(subscription.getDefaultPaymentMethod())
                .success(true)
                .metadata(subscription.getMetadata())
                .build();
    }

    private GatewayWebhookEvent.NormalizedEventType mapToNormalizedEventType(String eventType) {
        return switch (eventType) {
            case "payment_intent.succeeded" -> GatewayWebhookEvent.NormalizedEventType.PAYMENT_SUCCEEDED;
            case "payment_intent.payment_failed" -> GatewayWebhookEvent.NormalizedEventType.PAYMENT_FAILED;
            case "payment_intent.canceled" -> GatewayWebhookEvent.NormalizedEventType.PAYMENT_CANCELED;
            case "payment_intent.requires_action" -> GatewayWebhookEvent.NormalizedEventType.PAYMENT_REQUIRES_ACTION;
            case "setup_intent.succeeded" -> GatewayWebhookEvent.NormalizedEventType.SETUP_SUCCEEDED;
            case "setup_intent.setup_failed" -> GatewayWebhookEvent.NormalizedEventType.SETUP_FAILED;
            case "charge.refunded", "refund.created" -> GatewayWebhookEvent.NormalizedEventType.REFUND_CREATED;
            case "refund.updated" -> GatewayWebhookEvent.NormalizedEventType.REFUND_SUCCEEDED;
            case "refund.failed" -> GatewayWebhookEvent.NormalizedEventType.REFUND_FAILED;
            case "account.updated" -> GatewayWebhookEvent.NormalizedEventType.ACCOUNT_UPDATED;
            case "capability.updated" -> GatewayWebhookEvent.NormalizedEventType.ACCOUNT_CAPABILITY_UPDATED;
            case "transfer.created" -> GatewayWebhookEvent.NormalizedEventType.TRANSFER_CREATED;
            case "transfer.updated" -> GatewayWebhookEvent.NormalizedEventType.TRANSFER_UPDATED;
            case "transfer.reversed" -> GatewayWebhookEvent.NormalizedEventType.TRANSFER_REVERSED;
            case "payout.paid" -> GatewayWebhookEvent.NormalizedEventType.PAYOUT_PAID;
            case "payout.failed" -> GatewayWebhookEvent.NormalizedEventType.PAYOUT_FAILED;
            case "customer.subscription.created" -> GatewayWebhookEvent.NormalizedEventType.SUBSCRIPTION_CREATED;
            case "customer.subscription.updated" -> GatewayWebhookEvent.NormalizedEventType.SUBSCRIPTION_UPDATED;
            case "customer.subscription.deleted" -> GatewayWebhookEvent.NormalizedEventType.SUBSCRIPTION_DELETED;
            case "invoice.paid" -> GatewayWebhookEvent.NormalizedEventType.INVOICE_PAID;
            case "invoice.payment_failed" -> GatewayWebhookEvent.NormalizedEventType.INVOICE_PAYMENT_FAILED;
            case "checkout.session.completed" -> GatewayWebhookEvent.NormalizedEventType.CHECKOUT_SESSION_COMPLETED;
            default -> GatewayWebhookEvent.NormalizedEventType.UNKNOWN;
        };
    }
}
