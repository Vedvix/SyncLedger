package com.vedvix.syncledger.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Mapping profile entity — org-scoped field mapping rules for invoice data extraction.
 * Each organization can have multiple profiles (default, vendor-specific, ERP-specific).
 *
 * @author vedvix
 */
@Entity
@Table(name = "mapping_profiles", indexes = {
    @Index(name = "idx_mapping_profiles_org", columnList = "organization_id"),
    @Index(name = "idx_mapping_profiles_erp", columnList = "erp_type"),
    @Index(name = "idx_mapping_profiles_default", columnList = "organization_id, is_default")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MappingProfile {

    @Id
    @Column(length = 100)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "vendor_pattern", length = 500)
    private String vendorPattern;

    @Builder.Default
    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Builder.Default
    @Column(name = "is_builtin")
    private Boolean isBuiltin = false;

    @Column(name = "erp_type", length = 20)
    private String erpType;

    @Column(name = "rules_json", columnDefinition = "TEXT", nullable = false)
    @Builder.Default
    private String rulesJson = "[]";

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
