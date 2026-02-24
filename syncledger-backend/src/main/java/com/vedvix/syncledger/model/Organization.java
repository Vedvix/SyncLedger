package com.vedvix.syncledger.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Organization entity for multi-tenant SaaS model.
 * Each client organization has their own isolated data.
 * 
 * @author vedvix
 */
@Entity
@Table(name = "organizations", indexes = {
    @Index(name = "idx_org_slug", columnList = "slug"),
    @Index(name = "idx_org_email", columnList = "emailAddress"),
    @Index(name = "idx_org_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(nullable = false, unique = true, length = 255)
    private String emailAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OrganizationStatus status = OrganizationStatus.ACTIVE;

    // Primary Contact
    @Column(length = 255)
    private String contactName;

    @Column(length = 255)
    private String contactEmail;

    @Column(length = 50)
    private String contactPhone;

    // Sage Integration Configuration (legacy — use ERP fields instead)
    @Column(length = 500)
    private String sageApiEndpoint;

    @Column(length = 500)
    private String sageApiKey;

    @Column
    @Builder.Default
    private Boolean sageAutoSync = true;

    // ─── Generic ERP Integration Configuration ──────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "erp_type", length = 20)
    @Builder.Default
    private ErpType erpType = ErpType.NONE;

    @Column(name = "erp_api_endpoint", length = 500)
    private String erpApiEndpoint;

    @Column(name = "erp_api_key_encrypted", length = 1000)
    private String erpApiKeyEncrypted;

    @Column(name = "erp_tenant_id", length = 255)
    private String erpTenantId;

    @Column(name = "erp_company_id", length = 255)
    private String erpCompanyId;

    @Column(name = "erp_auto_sync")
    @Builder.Default
    private Boolean erpAutoSync = true;

    @Column(name = "erp_config_json", columnDefinition = "TEXT")
    private String erpConfigJson;

    // AWS Resource References
    @Column(name = "s3_folder_path", length = 500)
    private String s3FolderPath;

    @Column(length = 255)
    private String sqsQueueName;

    // Billing/Subscription (for future use)
    @Column(length = 50)
    private String subscriptionPlan;

    @Column
    private LocalDateTime subscriptionExpiresAt;

    // Microsoft Graph API Credentials (per-organization)
    @Column(length = 500)
    private String msClientId;

    @Column(name = "ms_client_secret_encrypted", length = 1000)
    private String msClientSecretEncrypted;

    @Column(length = 500)
    private String msTenantId;

    @Column(length = 255)
    private String msMailboxEmail;

    @Column
    @Builder.Default
    private Boolean msCredentialsVerified = false;

    @Column
    private LocalDateTime msCredentialsVerifiedAt;

    // Subscription relationship
    @OneToOne(mappedBy = "organization", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Subscription subscription;

    // Audit fields
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private Long createdBy;

    @UpdateTimestamp
    @Column
    private LocalDateTime updatedAt;

    @Column
    private Long updatedBy;

    /**
     * Get display name with status indicator
     */
    public String getDisplayName() {
        return status == OrganizationStatus.ACTIVE ? name : name + " (Inactive)";
    }
}
