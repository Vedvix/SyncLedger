package com.vedvix.syncledger.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for Vendor entity.
 * 
 * @author vedvix
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorDTO {
    private Long id;
    private Long organizationId;
    private String organizationName;
    
    // Identification
    private String name;
    private String code;
    
    // Contact
    private String address;
    private String email;
    private String phone;
    private String contactPerson;
    private String website;
    
    // Tax & Payment
    private String taxId;
    private String paymentTerms;
    private String currency;
    
    // Status
    private String status;
    private String notes;
    
    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Analytics (populated when requested)
    private VendorAnalyticsDTO analytics;
}
