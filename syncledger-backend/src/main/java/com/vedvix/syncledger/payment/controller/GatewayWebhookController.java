package com.vedvix.syncledger.payment.controller;

import com.vedvix.syncledger.payment.gateway.PaymentGateway;
import com.vedvix.syncledger.payment.gateway.PaymentGatewayFactory;
import com.vedvix.syncledger.payment.gateway.dto.GatewayWebhookEvent;
import com.vedvix.syncledger.payment.webhook.WebhookEventDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

/**
 * Gateway-agnostic Webhook Controller.
 * Routes webhook events to the appropriate handlers regardless of payment gateway.
 * 
 * This controller supports multiple payment gateways:
 * - /api/v2/webhooks/stripe - Stripe webhooks
 * - /api/v2/webhooks/paypal - PayPal webhooks (future)
 * - /api/v2/webhooks/square - Square webhooks (future)
 * 
 * Each gateway's webhook is parsed and normalized to a GatewayWebhookEvent,
 * then dispatched to the appropriate handlers.
 */
@RestController
@RequestMapping("/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class GatewayWebhookController {

    private final PaymentGatewayFactory gatewayFactory;
    private final WebhookEventDispatcher eventDispatcher;

    /**
     * Handle Stripe webhooks.
     */
    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature,
            HttpServletRequest request) {
        
        return processWebhook("stripe", payload, signature, request);
    }

    /**
     * Handle Stripe Connect webhooks.
     */
    @PostMapping("/stripe/connect")
    public ResponseEntity<String> handleStripeConnectWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature,
            HttpServletRequest request) {
        
        // Connect webhooks use a different secret but same gateway
        return processWebhook("stripe", payload, signature, request);
    }

    /**
     * Handle PayPal webhooks (future implementation).
     */
    @PostMapping("/paypal")
    public ResponseEntity<String> handlePayPalWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "PAYPAL-TRANSMISSION-SIG", required = false) String signature,
            HttpServletRequest request) {
        
        return processWebhook("paypal", payload, signature, request);
    }

    /**
     * Handle Square webhooks (future implementation).
     */
    @PostMapping("/square")
    public ResponseEntity<String> handleSquareWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Square-Signature", required = false) String signature,
            HttpServletRequest request) {
        
        return processWebhook("square", payload, signature, request);
    }

    /**
     * Generic webhook processor for any gateway.
     */
    private ResponseEntity<String> processWebhook(
            String gatewayId,
            String payload,
            String signature,
            HttpServletRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("gateway", gatewayId);
        MDC.put("endpoint", gatewayId + "-webhook");

        try {
            log.info("Received {} webhook from IP: {}", gatewayId, getClientIP(request));

            // Get the gateway
            Optional<PaymentGateway> gatewayOpt = gatewayFactory.findGateway(gatewayId);
            if (gatewayOpt.isEmpty()) {
                log.warn("No gateway implementation found for: {}", gatewayId);
                return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                        .body("Gateway not supported: " + gatewayId);
            }
            
            PaymentGateway gateway = gatewayOpt.get();

            // Verify webhook signature
            boolean isValid = gateway.verifyWebhookSignature(payload, signature);
            if (!isValid) {
                log.error("Invalid {} webhook signature", gatewayId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Invalid signature");
            }

            // Parse to normalized event
            GatewayWebhookEvent event = gateway.parseWebhookEvent(payload, signature);
            log.info("Parsed {} webhook event: {} ({})", 
                    gatewayId, event.getEventId(), event.getEventType());

            // Dispatch to handlers
            eventDispatcher.dispatch(event);

            return ResponseEntity.ok("Event processed successfully");

        } catch (Exception e) {
            log.error("Error processing {} webhook: {}", gatewayId, e.getMessage(), e);
            // Return 200 to prevent retries for unrecoverable errors
            return ResponseEntity.ok("Error logged for investigation");

        } finally {
            MDC.clear();
        }
    }

    private String getClientIP(HttpServletRequest request) {
        String header = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(header)) {
            return header.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
