package com.vedvix.syncledger.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Invoice line item entity representing individual items on an invoice.
 * 
 * @author vedvix
 */
@Entity
@Table(name = "invoice_line_items", indexes = {
    @Index(name = "idx_line_item_invoice", columnList = "invoice_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(nullable = false)
    private Integer lineNumber;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String itemCode;

    @Column(length = 50)
    private String unit;

    @Column(precision = 15, scale = 4)
    private BigDecimal quantity;

    @Column(precision = 15, scale = 4)
    private BigDecimal unitPrice;

    @Column(precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Column(precision = 15, scale = 2)
    private BigDecimal taxAmount;

    @Column(precision = 15, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal lineTotal;

    @Column(length = 100)
    private String glAccountCode;

    @Column(length = 100)
    private String costCenter;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Calculate line total from quantity and unit price.
     */
    public BigDecimal calculateLineTotal() {
        if (quantity == null || unitPrice == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal subtotal = quantity.multiply(unitPrice);
        if (discountAmount != null) {
            subtotal = subtotal.subtract(discountAmount);
        }
        if (taxAmount != null) {
            subtotal = subtotal.add(taxAmount);
        }
        return subtotal;
    }
}
