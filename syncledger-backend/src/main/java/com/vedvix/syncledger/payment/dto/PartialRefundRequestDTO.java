package com.vedvix.syncledger.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class PartialRefundRequestDTO {

    private String orderShortId;
    private List<Long> itemIds;
    private Boolean deliveryFee;
    private Boolean tip;
}
