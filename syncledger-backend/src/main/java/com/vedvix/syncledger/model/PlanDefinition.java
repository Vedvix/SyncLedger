package com.vedvix.syncledger.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Admin-editable subscription plan catalog.
 * Super Admins can create / update / deactivate plans without code changes.
 *
 * @author vedvix
 */
@Entity
@Table(name = "plan_definitions", indexes = {
        @Index(name = "idx_plan_definitions_active", columnList = "active"),
        @Index(name = "idx_plan_definitions_sort", columnList = "sortOrder")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_key", nullable = false, unique = true, length = 50)
    private String planKey;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(length = 500)
    private String description;

    @Column(name = "monthly_price", nullable = false)
    @Builder.Default
    private Long monthlyPrice = 0L;

    @Column(name = "annual_price", nullable = false)
    @Builder.Default
    private Long annualPrice = 0L;

    @Column(name = "invoices_per_month", nullable = false, length = 30)
    @Builder.Default
    private String invoicesPerMonth = "0";

    @Column(name = "max_users", nullable = false, length = 30)
    @Builder.Default
    private String maxUsers = "0";

    @Column(name = "max_organizations", nullable = false, length = 30)
    @Builder.Default
    private String maxOrganizations = "0";

    @Column(name = "max_email_inboxes", nullable = false, length = 30)
    @Builder.Default
    private String maxEmailInboxes = "0";

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String storage = "0";

    @Column(name = "approval_type", nullable = false, length = 50)
    @Builder.Default
    private String approvalType = "Basic";

    @Column(name = "support_level", nullable = false, length = 100)
    @Builder.Default
    private String supportLevel = "Email";

    @Column(name = "uptime_sla", nullable = false, length = 20)
    @Builder.Default
    private String uptimeSla = "99.5%";

    @Column(nullable = false)
    @Builder.Default
    private Boolean highlight = false;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column
    private LocalDateTime updatedAt;
}
