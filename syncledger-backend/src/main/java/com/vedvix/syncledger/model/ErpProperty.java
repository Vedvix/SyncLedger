package com.vedvix.syncledger.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Generic ERP property storage. Each row is a single key-value pair scoped to
 * an organization + ERP type. Secret values are encrypted by the application layer.
 *
 * @author vedvix
 */
@Entity
@Table(name = "erp_properties",
       uniqueConstraints = @UniqueConstraint(
               name = "uq_erp_prop_org_type_key",
               columnNames = {"organization_id", "erp_type", "property_key"}
       ),
       indexes = {
               @Index(name = "idx_erp_props_org", columnList = "organization_id"),
               @Index(name = "idx_erp_props_org_type", columnList = "organization_id, erp_type")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErpProperty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "erp_type", nullable = false, length = 20)
    private ErpType erpType;

    @Column(name = "property_key", nullable = false, length = 100)
    private String propertyKey;

    @Column(name = "property_value", length = 2000)
    private String propertyValue;

    @Column(name = "is_encrypted", nullable = false)
    @Builder.Default
    private Boolean isEncrypted = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column
    private LocalDateTime updatedAt;
}
