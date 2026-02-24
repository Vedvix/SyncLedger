package com.vedvix.syncledger.notification.exception;

import lombok.Getter;

/**
 * Exception thrown by notification providers (SES, Twilio, FCM, etc.)
 * when a delivery attempt fails.
 *
 * @author vedvix
 */
@Getter
public class ProviderException extends RuntimeException {

    private final String errorCode;
    private final boolean retryable;

    public ProviderException(String errorCode, String message, boolean retryable) {
        super(message);
        this.errorCode = errorCode;
        this.retryable = retryable;
    }

    public ProviderException(String errorCode, String message, boolean retryable, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.retryable = retryable;
    }

    public ProviderException(String errorCode, String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.errorCode = errorCode;
        this.retryable = retryable;
    }
}
