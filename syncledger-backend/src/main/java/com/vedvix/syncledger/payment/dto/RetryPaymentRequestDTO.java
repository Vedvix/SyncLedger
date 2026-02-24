package com.vedvix.syncledger.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RetryPaymentRequestDTO {

    private String stripePaymentIntentId;
    private UUID orderUuid;
}
