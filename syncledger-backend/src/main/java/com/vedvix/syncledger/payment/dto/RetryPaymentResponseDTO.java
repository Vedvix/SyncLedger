package com.vedvix.syncledger.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RetryPaymentResponseDTO {

    private String stripePaymentIntentId;
    private String clientSecret;
    private String status;
    private String message;
}
