package com.vedvix.syncledger.service;

import com.vedvix.syncledger.dto.*;
import com.vedvix.syncledger.exception.BadRequestException;
import com.vedvix.syncledger.exception.ForbiddenException;
import com.vedvix.syncledger.exception.ResourceNotFoundException;
import com.vedvix.syncledger.model.Organization;
import com.vedvix.syncledger.model.User;
import com.vedvix.syncledger.model.UserRole;
import com.vedvix.syncledger.repository.OrganizationRepository;
import com.vedvix.syncledger.repository.UserRepository;
import com.vedvix.syncledger.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for User management with multi-tenant support.
 * 
 * @author vedvix
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Get users for current user's organization (or all for Super Admin).
     */
    @Transactional(readOnly = true)
    public PagedResponse<UserDTO> getUsers(Pageable pageable, String search, UserPrincipal currentUser) {
        Page<User> users;

        if (currentUser.isSuperAdmin()) {
            // Super Admin sees all users
            if (search != null && !search.isEmpty()) {
                users = userRepository.searchUsers(search, pageable);
            } else {
                users = userRepository.findAll(pageable);
            }
        } else {
            // Org users see only their organization's users
            Long orgId = currentUser.getOrganizationId();
            if (search != null && !search.isEmpty()) {
                users = userRepository.searchUsersInOrganization(orgId, search, pageable);
            } else {
                users = userRepository.findByOrganization_Id(orgId, pageable);
            }
        }

        List<UserDTO> content = users.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        Page<UserDTO> userDTOs = users.map(this::mapToDTO);
        return PagedResponse.from(userDTOs);
    }

    /**
     * Get user by ID (with org access check).
     */
    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id, UserPrincipal currentUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        validateUserAccess(user, currentUser);
        return mapToDTO(user);
    }

    /**
     * Create a new user in organization.
     */
    @Transactional
    public UserDTO createUser(CreateUserRequest request, UserPrincipal currentUser) {
        // Validate email uniqueness
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        // Determine organization
        Organization organization;
        UserRole role = UserRole.valueOf(request.getRole().toUpperCase());

        if (currentUser.isSuperAdmin()) {
            // Super Admin can create users in any org or platform-level users
            if (role == UserRole.SUPER_ADMIN) {
                organization = null;
            } else {
                if (request.getOrganizationId() == null) {
                    throw new BadRequestException("Organization ID required for non-Super Admin users");
                }
                organization = organizationRepository.findById(request.getOrganizationId())
                        .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", request.getOrganizationId()));
            }
        } else if (currentUser.isAdmin()) {
            // Admin can only create users in their organization
            organization = organizationRepository.findById(currentUser.getOrganizationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", currentUser.getOrganizationId()));
            
            // Admin cannot create Super Admin or Admin
            if (role == UserRole.SUPER_ADMIN || role == UserRole.ADMIN) {
                throw new ForbiddenException("You cannot create users with this role");
            }
        } else {
            throw new ForbiddenException("You don't have permission to create users");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(role)
                .organization(organization)
                .isActive(true)
                .build();

        userRepository.save(user);
        log.info("User created: {} by {}", user.getEmail(), currentUser.getEmail());

        return mapToDTO(user);
    }

    /**
     * Update user.
     */
    @Transactional
    public UserDTO updateUser(Long id, UpdateUserRequest request, UserPrincipal currentUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        validateUserAccess(user, currentUser);

        // Update fields
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getJobTitle() != null) {
            user.setJobTitle(request.getJobTitle());
        }
        if (request.getDepartment() != null) {
            user.setDepartment(request.getDepartment());
        }

        // Role changes require elevated permissions
        if (request.getRole() != null) {
            UserRole newRole = request.getRole();
            if (!currentUser.hasPrivilege(newRole)) {
                throw new ForbiddenException("You cannot assign this role");
            }
            user.setRole(newRole);
        }

        // Active status
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }

        userRepository.save(user);
        log.info("User updated: {} by {}", user.getEmail(), currentUser.getEmail());

        return mapToDTO(user);
    }

    /**
     * Delete (deactivate) user.
     */
    @Transactional
    public void deleteUser(Long id, UserPrincipal currentUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        validateUserAccess(user, currentUser);

        // Prevent self-deletion
        if (user.getId().equals(currentUser.getId())) {
            throw new BadRequestException("You cannot delete your own account");
        }

        // Soft delete
        user.setIsActive(false);
        userRepository.save(user);
        
        log.info("User deactivated: {} by {}", user.getEmail(), currentUser.getEmail());
    }

    /**
     * Validate current user can access target user.
     */
    private void validateUserAccess(User targetUser, UserPrincipal currentUser) {
        if (currentUser.isSuperAdmin()) {
            return; // Super Admin can access all users
        }

        // Check same organization
        Long targetOrgId = targetUser.getOrganization() != null ? targetUser.getOrganization().getId() : null;
        if (!currentUser.canAccessOrganization(targetOrgId)) {
            throw new ForbiddenException("You don't have access to this user");
        }
    }

    private UserDTO mapToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .isActive(user.getIsActive())
                .phone(user.getPhone())
                .department(user.getDepartment())
                .jobTitle(user.getJobTitle())
                .organizationId(user.getOrganization() != null ? user.getOrganization().getId() : null)
                .organizationSlug(user.getOrganization() != null ? user.getOrganization().getSlug() : null)
                .organizationName(user.getOrganization() != null ? user.getOrganization().getName() : null)
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}

