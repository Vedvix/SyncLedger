package com.vedvix.syncledger.payment.gateway.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for address verification (AVS) results.
 */
@Data
@Builder
public class AddressVerificationResult {
    
    /**
     * Payment method ID checked
     */
    private String paymentMethodId;
    
    /**
     * Address line 1 check result: pass, fail, unavailable, unchecked
     */
    private String addressLine1Check;
    
    /**
     * Postal code check result: pass, fail, unavailable, unchecked
     */
    private String postalCodeCheck;
    
    /**
     * CVC check result: pass, fail, unavailable, unchecked
     */
    private String cvcCheck;
    
    /**
     * Overall verification passed based on rules
     */
    private boolean verified;
    
    /**
     * Message explaining verification outcome
     */
    private String message;
    
    /**
     * Evaluate if the verification should be accepted.
     * Applies standard AVS rules.
     */
    public static AddressVerificationResult evaluate(
            String paymentMethodId,
            String addressLine1Check,
            String postalCodeCheck,
            String cvcCheck) {
        
        AddressVerificationResultBuilder builder = AddressVerificationResult.builder()
                .paymentMethodId(paymentMethodId)
                .addressLine1Check(addressLine1Check)
                .postalCodeCheck(postalCodeCheck)
                .cvcCheck(cvcCheck);
        
        // Both pass - accept
        if ("pass".equals(addressLine1Check) && "pass".equals(postalCodeCheck)) {
            return builder.verified(true).message("AVS verification passed").build();
        }
        
        // Both unavailable - international card or unsupported issuer, accept
        if ("unavailable".equals(addressLine1Check) && "unavailable".equals(postalCodeCheck)) {
            return builder.verified(true).message("AVS unavailable - international card").build();
        }
        
        // Either unchecked - accept (not our failure)
        if ("unchecked".equals(addressLine1Check) || "unchecked".equals(postalCodeCheck)) {
            return builder.verified(true).message("AVS unchecked by issuer").build();
        }
        
        // At least one pass - accept with caution
        if ("pass".equals(addressLine1Check) || "pass".equals(postalCodeCheck)) {
            return builder.verified(true).message("Partial AVS match - review recommended").build();
        }
        
        // Both fail - reject
        if ("fail".equals(addressLine1Check) && "fail".equals(postalCodeCheck)) {
            return builder.verified(false).message("AVS verification failed - address mismatch").build();
        }
        
        // Default: accept (be lenient for edge cases)
        return builder.verified(true).message("AVS check inconclusive - accepted").build();
    }
}
