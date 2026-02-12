package com.vedvix.syncledger.dto;

import com.vedvix.syncledger.model.InvoiceStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for updating invoice data.
 * 
 * @author vedvix
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateInvoiceRequest {

    @Size(max = 100, message = "Invoice number must be less than 100 characters")
    private String invoiceNumber;

    @Size(max = 100, message = "PO number must be less than 100 characters")
    private String poNumber;

    @Size(max = 255, message = "Vendor name must be less than 255 characters")
    private String vendorName;

    @Size(max = 255, message = "Vendor address must be less than 255 characters")
    private String vendorAddress;

    @Email(message = "Invalid vendor email format")
    private String vendorEmail;

    @Size(max = 50, message = "Vendor phone must be less than 50 characters")
    private String vendorPhone;

    @DecimalMin(value = "0.00", message = "Subtotal must be positive")
    private BigDecimal subtotal;

    @DecimalMin(value = "0.00", message = "Tax amount must be positive")
    private BigDecimal taxAmount;

    @DecimalMin(value = "0.00", message = "Discount amount must be positive")
    private BigDecimal discountAmount;

    @DecimalMin(value = "0.00", message = "Shipping amount must be positive")
    private BigDecimal shippingAmount;

    @DecimalMin(value = "0.00", message = "Total amount must be positive")
    private BigDecimal totalAmount;

    private LocalDate invoiceDate;
    
    private LocalDate dueDate;

    @Size(max = 500, message = "Review notes must be less than 500 characters")
    private String reviewNotes;

    private List<InvoiceLineItemDTO> lineItems;

    // Mapping Fields
    @Size(max = 50, message = "GL account must be less than 50 characters")
    private String glAccount;

    @Size(max = 255, message = "Project must be less than 255 characters")
    private String project;

    @Size(max = 100, message = "Item category must be less than 100 characters")
    private String itemCategory;

    @Size(max = 500, message = "Location must be less than 500 characters")
    private String location;

    @Size(max = 100, message = "Cost center must be less than 100 characters")
    private String costCenter;
}
