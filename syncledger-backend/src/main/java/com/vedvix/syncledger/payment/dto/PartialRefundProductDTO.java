package com.vedvix.syncledger.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PartialRefundProductDTO {
    private Long itemId;
    private Integer quantity;
}
