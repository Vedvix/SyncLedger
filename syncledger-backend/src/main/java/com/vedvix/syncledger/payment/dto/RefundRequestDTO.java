package com.vedvix.syncledger.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record RefundRequestDTO(
        @NotBlank String orderShortId
) {}
