package com.vedvix.syncledger.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * Request DTO for creating or updating a vendor.
 * 
 * @author vedvix
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorRequest {
    private String name;
    private String code;
    private String address;
    private String email;
    private String phone;
    private String contactPerson;
    private String website;
    private String taxId;
    private String paymentTerms;
    private String currency;
    private String status;
    private String notes;
}
