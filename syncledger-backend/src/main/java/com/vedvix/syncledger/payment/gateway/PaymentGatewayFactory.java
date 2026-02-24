package com.vedvix.syncledger.payment.gateway;

import com.vedvix.syncledger.payment.gateway.exception.PaymentGatewayException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory for obtaining PaymentGateway instances.
 * Supports multiple gateways and allows dynamic gateway selection.
 *
 * <p>Usage:
 * <pre>
 * // Get default gateway
 * PaymentGateway gateway = factory.getDefaultGateway();
 *
 * // Get specific gateway
 * PaymentGateway stripeGateway = factory.getGateway("stripe");
 *
 * // Check available gateways
 * List&lt;String&gt; available = factory.getAvailableGateways();
 * </pre>
 *
 * <p>To add a new gateway:
 * <ol>
 *   <li>Implement PaymentGateway interface</li>
 *   <li>Annotate with @Service</li>
 *   <li>The factory will automatically discover it via Spring injection</li>
 * </ol>
 */
@Slf4j
@Component
public class PaymentGatewayFactory {

    private final Map<String, PaymentGateway> gatewayMap;
    private final PaymentGateway primaryGateway;

    @Value("${payment.gateway.default:stripe}")
    private String defaultGatewayId;

    /**
     * Constructor - Spring injects all PaymentGateway implementations.
     * The @Primary gateway is also injected separately for fallback.
     *
     * @param gateways all available gateway implementations
     * @param primaryGateway the primary (default) gateway
     */
    public PaymentGatewayFactory(List<PaymentGateway> gateways, PaymentGateway primaryGateway) {
        this.gatewayMap = gateways.stream()
                .collect(Collectors.toMap(
                        PaymentGateway::getGatewayId,
                        Function.identity(),
                        (existing, replacement) -> existing // Keep first if duplicate
                ));
        this.primaryGateway = primaryGateway;
        
        log.info("PaymentGatewayFactory initialized with {} gateways: {}", 
                gatewayMap.size(), gatewayMap.keySet());
    }

    /**
     * Get the default payment gateway based on configuration.
     * Falls back to the @Primary gateway if configured gateway not found.
     *
     * @return the default PaymentGateway
     */
    public PaymentGateway getDefaultGateway() {
        PaymentGateway gateway = gatewayMap.get(defaultGatewayId);
        
        if (gateway == null) {
            log.warn("Configured default gateway '{}' not found, using primary gateway", defaultGatewayId);
            return primaryGateway;
        }
        
        if (!gateway.isAvailable()) {
            log.warn("Default gateway '{}' is not available, using primary gateway", defaultGatewayId);
            return primaryGateway;
        }
        
        return gateway;
    }

    /**
     * Get a specific payment gateway by ID.
     *
     * @param gatewayId the gateway identifier (e.g., "stripe", "paypal")
     * @return the PaymentGateway
     * @throws PaymentGatewayException if gateway not found or not available
     */
    public PaymentGateway getGateway(String gatewayId) {
        PaymentGateway gateway = gatewayMap.get(gatewayId);
        
        if (gateway == null) {
            throw new PaymentGatewayException("Payment gateway not found: " + gatewayId);
        }
        
        if (!gateway.isAvailable()) {
            throw new PaymentGatewayException("Payment gateway not available: " + gatewayId);
        }
        
        return gateway;
    }

    /**
     * Get a specific payment gateway by ID, returning Optional.
     *
     * @param gatewayId the gateway identifier
     * @return Optional containing the gateway, or empty if not found/available
     */
    public Optional<PaymentGateway> findGateway(String gatewayId) {
        PaymentGateway gateway = gatewayMap.get(gatewayId);
        
        if (gateway == null || !gateway.isAvailable()) {
            return Optional.empty();
        }
        
        return Optional.of(gateway);
    }

    /**
     * Get list of all available gateway IDs.
     *
     * @return list of gateway identifiers that are currently available
     */
    public List<String> getAvailableGateways() {
        return gatewayMap.values().stream()
                .filter(PaymentGateway::isAvailable)
                .map(PaymentGateway::getGatewayId)
                .toList();
    }

    /**
     * Get list of all registered gateways (including unavailable ones).
     *
     * @return list of all gateway identifiers
     */
    public List<String> getAllGateways() {
        return gatewayMap.keySet().stream().toList();
    }

    /**
     * Check if a specific gateway is available.
     *
     * @param gatewayId the gateway identifier
     * @return true if the gateway exists and is available
     */
    public boolean isGatewayAvailable(String gatewayId) {
        PaymentGateway gateway = gatewayMap.get(gatewayId);
        return gateway != null && gateway.isAvailable();
    }

    /**
     * Get gateway information for admin/status endpoints.
     *
     * @return map of gateway info (id -> status)
     */
    public Map<String, GatewayStatus> getGatewayStatuses() {
        return gatewayMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> new GatewayStatus(
                                e.getValue().getGatewayId(),
                                e.getValue().getGatewayName(),
                                e.getValue().isAvailable(),
                                e.getKey().equals(defaultGatewayId)
                        )
                ));
    }

    /**
     * Status information for a gateway.
     */
    public record GatewayStatus(
            String id,
            String name,
            boolean available,
            boolean isDefault
    ) {}
}
