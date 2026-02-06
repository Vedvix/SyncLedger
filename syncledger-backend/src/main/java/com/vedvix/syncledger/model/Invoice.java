package com.vedvix.syncledger.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Invoice entity representing extracted invoice data.
 * 
 * @author vedvix
 */
@Entity
@Table(name = "invoices", indexes = {
    @Index(name = "idx_invoice_number", columnList = "invoiceNumber"),
    @Index(name = "idx_invoice_status", columnList = "status"),
    @Index(name = "idx_invoice_vendor", columnList = "vendorName"),
    @Index(name = "idx_invoice_date", columnList = "invoiceDate"),
    @Index(name = "idx_invoice_due_date", columnList = "dueDate")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== Invoice Identification ====================
    
    @Column(nullable = false, length = 100)
    private String invoiceNumber;

    @Column(length = 100)
    private String poNumber;

    // ==================== Vendor Information ====================
    
    @Column(nullable = false, length = 255)
    private String vendorName;

    @Column(length = 255)
    private String vendorAddress;

    @Column(length = 100)
    private String vendorEmail;

    @Column(length = 50)
    private String vendorPhone;

    @Column(length = 50)
    private String vendorTaxId;

    // ==================== Financial Details ====================
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal shippingAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 3)
    @Builder.Default
    private String currency = "USD";

    // ==================== Dates ====================
    
    @Column(nullable = false)
    private LocalDate invoiceDate;

    @Column
    private LocalDate dueDate;

    @Column
    private LocalDate receivedDate;

    // ==================== Status & Processing ====================
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.PENDING;

    @Column(precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    @Column
    @Builder.Default
    private Boolean requiresManualReview = false;

    @Column(length = 500)
    private String reviewNotes;

    // ==================== File Storage ====================
    
    @Column(nullable = false, length = 500)
    private String originalFileName;

    @Column(nullable = false, length = 500)
    private String s3Key;

    @Column(length = 500)
    private String s3Url;

    @Column
    private Long fileSizeBytes;

    @Column(length = 50)
    private String mimeType;

    @Column
    private Integer pageCount;

    // ==================== Email Source ====================
    
    @Column(length = 500)
    private String sourceEmailId;

    @Column(length = 255)
    private String sourceEmailFrom;

    @Column(length = 500)
    private String sourceEmailSubject;

    @Column
    private LocalDateTime sourceEmailReceivedAt;

    // ==================== Extraction Metadata ====================
    
    @Column(length = 50)
    private String extractionMethod;

    @Column
    private LocalDateTime extractedAt;

    @Column
    private Integer extractionDurationMs;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String rawExtractedData;

    // ==================== Sage Integration ====================
    
    @Column(length = 100)
    private String sageInvoiceId;

    @Column(length = 100)
    private String sageVendorId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SyncStatus syncStatus;

    @Column
    private LocalDateTime lastSyncAttempt;

    @Column
    private Integer syncAttemptCount;

    @Column(length = 500)
    private String syncErrorMessage;

    // ==================== Line Items ====================
    
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InvoiceLineItem> lineItems = new ArrayList<>();

    // ==================== Approvals ====================
    
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Approval> approvals = new ArrayList<>();

    // ==================== Audit Fields ====================
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by_id")
    private User processedBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // ==================== Helper Methods ====================
    
    /**
     * Add a line item to this invoice.
     */
    public void addLineItem(InvoiceLineItem lineItem) {
        lineItems.add(lineItem);
        lineItem.setInvoice(this);
    }

    /**
     * Remove a line item from this invoice.
     */
    public void removeLineItem(InvoiceLineItem lineItem) {
        lineItems.remove(lineItem);
        lineItem.setInvoice(null);
    }

    /**
     * Add an approval record.
     */
    public void addApproval(Approval approval) {
        approvals.add(approval);
        approval.setInvoice(this);
    }

    /**
     * Check if invoice is editable.
     */
    public boolean isEditable() {
        return status == InvoiceStatus.PENDING || status == InvoiceStatus.UNDER_REVIEW;
    }

    /**
     * Check if invoice can be synced to Sage.
     */
    public boolean canSync() {
        return status == InvoiceStatus.APPROVED && syncStatus != SyncStatus.SUCCESS;
    }

    /**
     * Calculate days until due date.
     */
    public Long getDaysUntilDue() {
        if (dueDate == null) return null;
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
    }

    /**
     * Check if invoice is overdue.
     */
    public boolean isOverdue() {
        return dueDate != null && LocalDate.now().isAfter(dueDate) 
               && status != InvoiceStatus.SYNCED;
    }
}
