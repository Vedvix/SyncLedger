package com.vedvix.syncledger.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for Invoice entity.
 * 
 * @author vedvix
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceDTO {

    private Long id;
    
    // Organization (Multi-Tenant)
    private Long organizationId;
    
    // Invoice Identification
    private String invoiceNumber;
    private String poNumber;
    
    // Vendor Information
    private String vendorName;
    private String vendorAddress;
    private String vendorEmail;
    private String vendorPhone;
    private String vendorTaxId;
    
    // Financial Details
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal shippingAmount;
    private BigDecimal totalAmount;
    private String currency;
    
    // Dates
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private LocalDate receivedDate;
    
    // Status & Processing
    private String status;
    private BigDecimal confidenceScore;
    private Boolean requiresManualReview;
    private String reviewNotes;
    
    // File Info
    private String originalFileName;
    private String s3Url;
    private Long fileSizeBytes;
    private Integer pageCount;
    
    // Email Source
    private String sourceEmailFrom;
    private String sourceEmailSubject;
    private LocalDateTime sourceEmailReceivedAt;
    
    // Extraction
    private String extractionMethod;
    private LocalDateTime extractedAt;
    
    // Sage Integration
    private String sageInvoiceId;
    private String syncStatus;
    private LocalDateTime lastSyncAttempt;
    private String syncErrorMessage;
    
    // Line Items
    private List<InvoiceLineItemDTO> lineItems;
    
    // Assignment
    private Long assignedToId;
    private String assignedToName;
    
    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Computed fields
    private Long daysUntilDue;
    private Boolean isOverdue;
    private Boolean isEditable;
}
