package com.vedvix.syncledger.security;

import com.vedvix.syncledger.model.User;
import com.vedvix.syncledger.model.UserRole;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Custom UserDetails implementation wrapping User entity.
 * Provides multi-tenant context through organization ID.
 * 
 * @author vedvix
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final String firstName;
    private final String lastName;
    private final UserRole role;
    private final Long organizationId;
    private final String organizationSlug;
    private final String organizationStatus;
    private final boolean isActive;
    private final boolean isLocked;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.role = user.getRole();
        this.organizationId = user.getOrganization() != null ? user.getOrganization().getId() : null;
        this.organizationSlug = user.getOrganization() != null ? user.getOrganization().getSlug() : null;
        this.organizationStatus = user.getOrganization() != null ? user.getOrganization().getStatus().name() : null;
        this.isActive = user.getIsActive();
        this.isLocked = user.isAccountLocked();
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    public static UserPrincipal create(User user) {
        return new UserPrincipal(user);
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !isLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }

    /**
     * Check if user is a Super Admin.
     */
    public boolean isSuperAdmin() {
        return role == UserRole.SUPER_ADMIN;
    }

    /**
     * Check if user is an Admin (org-level).
     */
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    /**
     * Check if user has privilege to manage at least this role.
     */
    public boolean hasPrivilege(UserRole targetRole) {
        return role.hasPrivilege(targetRole);
    }

    /**
     * Check if user can access given organization.
     */
    public boolean canAccessOrganization(Long orgId) {
        if (isSuperAdmin()) {
            return true; // Super Admin can access all organizations
        }
        return organizationId != null && organizationId.equals(orgId);
    }

    /**
     * Get full name.
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
