package com.vedvix.syncledger.payment.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for PaymentIntent creation.
 * 
 * Frontend Integration Guide:
 * 
 * 1. Check 'success' field first - if false, display errorMessage
 * 
 * 2. Check 'autoConfirmed' field:
 *    - If TRUE (test mode): Payment already confirmed, skip Stripe Elements
 *      - Check 'paymentStatus' for current state ("succeeded", "processing", etc.)
 *      - Proceed to order confirmation
 *    
 *    - If FALSE (production): Payment requires frontend confirmation
 *      - Use 'clientSecret' with Stripe Elements to complete payment
 *      - Call stripe.confirmPayment() or stripe.confirmCardPayment()
 * 
 * Example frontend code:
 * <pre>
 * const response = await createOrder(orderData);
 * 
 * if (!response.success) {
 *   showError(response.errorMessage);
 *   return;
 * }
 * 
 * if (response.autoConfirmed) {
 *   // Test mode - payment already done
 *   navigateToConfirmation(response.stripePaymentIntentId);
 * } else {
 *   // Production - complete with Stripe Elements
 *   const { error } = await stripe.confirmPayment({
 *     elements,
 *     clientSecret: response.clientSecret,
 *     confirmParams: { return_url: window.location.origin + '/order/confirm' }
 *   });
 *   if (error) showError(error.message);
 * }
 * </pre>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentIntentResponseDTO {

    /**
     * Whether the API call was successful.
     */
    private Boolean success;
    
    /**
     * The Stripe PaymentIntent ID (e.g., "pi_xxx").
     */
    private String stripePaymentIntentId;
    
    /**
     * Client secret for frontend to complete payment via Stripe Elements.
     * Use this with stripe.confirmPayment() or stripe.confirmCardPayment().
     */
    private String clientSecret;
    
    /**
     * Error message if success=false.
     */
    private String errorMessage;
    
    /**
     * Indicates if the payment was auto-confirmed (test mode).
     * 
     * TRUE: Payment already confirmed - skip Stripe Elements confirmation
     * FALSE/null: Frontend must complete payment using clientSecret
     */
    private Boolean autoConfirmed;
    
    /**
     * Current PaymentIntent status from Stripe.
     * 
     * Common values:
     * - "requires_payment_method": Awaiting payment method
     * - "requires_confirmation": Awaiting confirmation
     * - "requires_action": Awaiting customer action (3DS, etc.)
     * - "processing": Payment is processing
     * - "succeeded": Payment completed successfully
     * - "canceled": Payment was canceled
     */
    private String paymentStatus;
}
