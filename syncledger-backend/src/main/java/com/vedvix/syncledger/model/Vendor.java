package com.vedvix.syncledger.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Vendor entity representing a supplier/vendor linked to one or more organizations.
 * Same vendor (by name) can exist independently in different organizations.
 * 
 * @author vedvix
 */
@Entity
@Table(name = "vendors", indexes = {
    @Index(name = "idx_vendor_normalized_name", columnList = "normalizedName"),
    @Index(name = "idx_vendor_status", columnList = "status"),
    @Index(name = "idx_vendor_tax_id", columnList = "taxId")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_vendor_org_normalized_name", columnNames = {"organization_id", "normalizedName"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== Organization (Multi-Tenant) ====================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    // ==================== Vendor Identification ====================

    @Column(nullable = false, length = 255)
    private String name;

    /**
     * Lowercased, trimmed version of name for duplicate detection.
     * Used in unique constraint with organization_id.
     */
    @Column(nullable = false, length = 255)
    private String normalizedName;

    @Column(length = 100)
    private String code;

    // ==================== Contact Information ====================

    @Column(length = 500)
    private String address;

    @Column(length = 255)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(length = 100)
    private String contactPerson;

    @Column(length = 255)
    private String website;

    // ==================== Tax & Payment ====================

    @Column(length = 50)
    private String taxId;

    @Column(length = 50)
    private String paymentTerms;

    @Column(length = 3)
    @Builder.Default
    private String currency = "USD";

    // ==================== Status ====================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private VendorStatus status = VendorStatus.ACTIVE;

    @Column(length = 500)
    private String notes;

    // ==================== Audit Fields ====================

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // ==================== Helper Methods ====================

    /**
     * Get organization ID safely.
     */
    public Long getOrganizationId() {
        return organization != null ? organization.getId() : null;
    }

    /**
     * Normalize a vendor name for matching.
     */
    public static String normalizeName(String name) {
        if (name == null) return "";
        return name.trim().toLowerCase()
                .replaceAll("\\s+", " ")           // collapse whitespace
                .replaceAll("[.,;:!?'\"()\\[\\]{}]", "") // remove punctuation
                .replaceAll("\\b(inc|llc|ltd|corp|co|company|enterprises|services|group)\\b", "") // remove suffixes
                .replaceAll("\\s+", " ")            // re-collapse
                .trim();
    }

    @PrePersist
    @PreUpdate
    private void ensureNormalizedName() {
        if (normalizedName == null || normalizedName.isEmpty()) {
            normalizedName = normalizeName(name);
        }
    }
}
