package com.vedvix.syncledger.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * User entity for SyncLedger application.
 * Multi-tenant: Users belong to an organization (except SUPER_ADMIN).
 * Users are created by Super Admin (for ADMIN) or Org Admin (for others).
 * 
 * @author vedvix
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_role", columnList = "role"),
    @Index(name = "idx_user_org", columnList = "organization_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Organization this user belongs to.
     * NULL for SUPER_ADMIN users (platform-level access).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(length = 500)
    private String profilePictureUrl;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String department;

    @Column(length = 200)
    private String jobTitle;

    @Column
    private LocalDateTime lastLoginAt;

    @Column
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @Column
    private LocalDateTime lockedUntil;

    @Column
    private String passwordResetToken;

    @Column
    private LocalDateTime passwordResetTokenExpiry;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    /**
     * Get full name of the user.
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Get organization ID (null for SUPER_ADMIN).
     */
    public Long getOrganizationId() {
        return organization != null ? organization.getId() : null;
    }

    /**
     * Get organization name (null for SUPER_ADMIN).
     */
    public String getOrganizationName() {
        return organization != null ? organization.getName() : null;
    }

    /**
     * Check if account is locked.
     */
    public boolean isAccountLocked() {
        return lockedUntil != null && LocalDateTime.now().isBefore(lockedUntil);
    }

    /**
     * Check if user is a Super Admin (platform-level access).
     */
    public boolean isSuperAdmin() {
        return role == UserRole.SUPER_ADMIN;
    }

    /**
     * Check if user is an Organization Admin.
     */
    public boolean isOrgAdmin() {
        return role == UserRole.ADMIN;
    }

    /**
     * Check if user has admin privileges (Super Admin or Org Admin).
     */
    public boolean isAdmin() {
        return role == UserRole.SUPER_ADMIN || role == UserRole.ADMIN;
    }

    /**
     * Check if user can approve invoices.
     */
    public boolean canApprove() {
        return role == UserRole.SUPER_ADMIN || role == UserRole.ADMIN || role == UserRole.APPROVER;
    }

    /**
     * Check if user can manage users.
     * SUPER_ADMIN can manage all users.
     * ADMIN can manage users within their organization.
     */
    public boolean canManageUsers() {
        return role == UserRole.SUPER_ADMIN || role == UserRole.ADMIN;
    }

    /**
     * Check if user can create another user with the given role.
     */
    public boolean canCreateUserWithRole(UserRole targetRole) {
        if (role == UserRole.SUPER_ADMIN) {
            return true; // Super Admin can create any role
        }
        if (role == UserRole.ADMIN) {
            // Org Admin can only create APPROVER and VIEWER
            return targetRole == UserRole.APPROVER || targetRole == UserRole.VIEWER;
        }
        return false;
    }
}
