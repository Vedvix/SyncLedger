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

    // Sage Integration Configuration
    @Column(length = 500)
    private String sageApiEndpoint;

    @Column(length = 500)
    private String sageApiKey;

    @Column
    @Builder.Default
    private Boolean sageAutoSync = true;

    // AWS Resource References
    @Column(length = 500)
    private String s3FolderPath;

    @Column(length = 255)
    private String sqsQueueName;

    // Billing/Subscription (for future use)
    @Column(length = 50)
    private String subscriptionPlan;

    @Column
    private LocalDateTime subscriptionExpiresAt;

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
