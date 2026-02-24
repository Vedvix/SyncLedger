package com.vedvix.syncledger.payment.gateway.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Response DTO representing a connected account from the gateway.
 */
@Data
@Builder
public class GatewayConnectedAccount {
    
    /**
     * Gateway-specific account ID (e.g., acct_xxx for Stripe)
     */
    private String accountId;
    
    /**
     * Account type: express, standard, custom
     */
    private String accountType;
    
    /**
     * Business email
     */
    private String email;
    
    /**
     * Business type
     */
    private String businessType;
    
    /**
     * Country code
     */
    private String country;
    
    /**
     * Whether the account can accept charges
     */
    private boolean chargesEnabled;
    
    /**
     * Whether the account can receive payouts
     */
    private boolean payoutsEnabled;
    
    /**
     * Whether onboarding is complete
     */
    private boolean detailsSubmitted;
    
    /**
     * List of currently due requirements
     */
    private List<String> currentlyDue;
    
    /**
     * List of past due requirements
     */
    private List<String> pastDue;
    
    /**
     * Bank account details (masked)
     */
    private ConnectedBankAccount bankAccount;
    
    /**
     * Additional metadata
     */
    private Map<String, String> metadata;
    
    /**
     * Whether the operation was successful
     */
    private boolean success;
    
    // ============================================
    // ALIAS METHODS
    // ============================================
    
    /**
     * Alias for accountId
     */
    public String getId() {
        return accountId;
    }
    
    /**
     * Error message if operation failed
     */
    private String errorMessage;
    
    /**
     * Bank account details for connected account
     */
    @Data
    @Builder
    public static class ConnectedBankAccount {
        private String bankName;
        private String accountHolderName;
        private String accountHolderType;
        private String last4;
        private String currency;
    }
}
