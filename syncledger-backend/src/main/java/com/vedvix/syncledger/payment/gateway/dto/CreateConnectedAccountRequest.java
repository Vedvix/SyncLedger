package com.vedvix.syncledger.payment.gateway.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for creating a connected account (for marketplace/platform).
 */
@Data
@Builder
public class CreateConnectedAccountRequest {
    
    /**
     * Type: express, standard, custom
     */
    private String accountType;
    
    /**
     * Country code (2-letter ISO)
     */
    private String country;
    
    /**
     * Business email
     */
    private String email;
    
    /**
     * Business type: individual, company, non_profit, government_entity
     */
    private String businessType;
    
    /**
     * Company/business name
     */
    private String businessName;
    
    /**
     * Business phone
     */
    private String businessPhone;
    
    /**
     * Tax ID / EIN
     */
    private String taxId;
    
    /**
     * Internal store ID
     */
    private Long storeId;
    
    /**
     * Internal store UUID
     */
    private String storeUuid;
    
    /**
     * Requested capabilities: card_payments, transfers
     */
    private List<String> capabilities;
    
    /**
     * Additional metadata
     */
    private Map<String, String> metadata;
}
