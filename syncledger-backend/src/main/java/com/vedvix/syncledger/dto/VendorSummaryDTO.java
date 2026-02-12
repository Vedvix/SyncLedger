package com.vedvix.syncledger.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Summary analytics across all vendors for an organization.
 * 
 * @author vedvix
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorSummaryDTO {
    private Long totalVendors;
    private Long activeVendors;
    private Long totalInvoicesAcrossVendors;
    private BigDecimal totalAmountAcrossVendors;
    private BigDecimal averageAmountPerVendor;
    
    // Top vendors by invoice count
    private List<TopVendorDTO> topVendorsByCount;
    // Top vendors by total amount
    private List<TopVendorDTO> topVendorsByAmount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopVendorDTO {
        private Long vendorId;
        private String vendorName;
        private Long invoiceCount;
        private BigDecimal totalAmount;
        private BigDecimal averageAmount;
    }
}
