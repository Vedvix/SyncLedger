package com.vedvix.syncledger.payment.gateway.exception;

/**
 * Exception thrown when a payment gateway operation fails.
 * This is a gateway-agnostic exception that wraps provider-specific errors.
 */
public class PaymentGatewayException extends RuntimeException {

    private final String gatewayErrorCode;
    private final String gatewayId;

    public PaymentGatewayException(String message) {
        super(message);
        this.gatewayErrorCode = null;
        this.gatewayId = null;
    }

    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
        this.gatewayErrorCode = null;
        this.gatewayId = null;
    }

    public PaymentGatewayException(String message, String gatewayId, String gatewayErrorCode) {
        super(message);
        this.gatewayId = gatewayId;
        this.gatewayErrorCode = gatewayErrorCode;
    }

    public PaymentGatewayException(String message, String gatewayId, String gatewayErrorCode, Throwable cause) {
        super(message, cause);
        this.gatewayId = gatewayId;
        this.gatewayErrorCode = gatewayErrorCode;
    }

    public String getGatewayErrorCode() {
        return gatewayErrorCode;
    }

    public String getGatewayId() {
        return gatewayId;
    }
}
