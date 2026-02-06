package com.vedvix.syncledger.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * Data Transfer Object for InvoiceLineItem entity.
 * 
 * @author vedvix
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceLineItemDTO {

    private Long id;
    private Integer lineNumber;
    private String description;
    private String itemCode;
    private String unit;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal lineTotal;
    private String glAccountCode;
    private String costCenter;
}
