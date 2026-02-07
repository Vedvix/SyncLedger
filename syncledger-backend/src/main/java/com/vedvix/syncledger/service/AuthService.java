package com.vedvix.syncledger.service;

import com.vedvix.syncledger.dto.*;
import com.vedvix.syncledger.exception.BadRequestException;
import com.vedvix.syncledger.exception.ResourceNotFoundException;
import com.vedvix.syncledger.exception.UnauthorizedException;
import com.vedvix.syncledger.model.Organization;
import com.vedvix.syncledger.model.User;
import com.vedvix.syncledger.model.UserRole;
import com.vedvix.syncledger.repository.OrganizationRepository;
import com.vedvix.syncledger.repository.UserRepository;
import com.vedvix.syncledger.security.JwtTokenProvider;
import com.vedvix.syncledger.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Authentication service handling login, registration, and token management.
 * Supports multi-tenant authentication with organization context.
 * 
 * @author vedvix
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    /**
     * Authenticate user and generate JWT tokens.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getEmail(),
                    request.getPassword()
                )
            );

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            
            // Update last login timestamp
            userRepository.updateLastLogin(userPrincipal.getId(), LocalDateTime.now());
            
            String accessToken = tokenProvider.generateToken(userPrincipal);
            String refreshToken = tokenProvider.generateRefreshToken(userPrincipal);

            log.info("User {} logged in successfully", request.getEmail());

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(3600L)
                    .user(mapToUserDTO(userPrincipal))
                    .build();

        } catch (AuthenticationException e) {
            // Increment failed login attempts
            userRepository.findByEmailIgnoreCase(request.getEmail())
                    .ifPresent(user -> {
                        userRepository.incrementFailedLoginAttempts(user.getId());
                        
                        // Lock account after 5 failed attempts
                        if (user.getFailedLoginAttempts() != null && user.getFailedLoginAttempts() >= 4) {
                            userRepository.lockAccount(user.getId(), LocalDateTime.now().plusMinutes(30));
                            log.warn("Account locked for user: {}", request.getEmail());
                        }
                    });
            
            log.warn("Failed login attempt for: {}", request.getEmail());
            throw new UnauthorizedException("Invalid email or password");
        }
    }

    /**
     * Refresh access token using refresh token.
     */
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken) || !tokenProvider.isRefreshToken(refreshToken)) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        Long userId = tokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (!user.getIsActive()) {
            throw new UnauthorizedException("User account is disabled");
        }

        UserPrincipal userPrincipal = UserPrincipal.create(user);
        String newAccessToken = tokenProvider.generateToken(userPrincipal);
        String newRefreshToken = tokenProvider.generateRefreshToken(userPrincipal);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(3600L)
                .user(mapToUserDTO(userPrincipal))
                .build();
    }

    /**
     * Register first Super Admin (only if no users exist).
     */
    @Transactional
    public AuthResponse registerSuperAdmin(CreateUserRequest request) {
        // Only allow if no Super Admin exists
        if (userRepository.countSuperAdmins() > 0) {
            throw new BadRequestException("Super Admin already exists. Use proper admin endpoints to create users.");
        }

        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        User superAdmin = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(UserRole.SUPER_ADMIN)
                .organization(null)  // Super Admin has no org
                .isActive(true)
                .build();

        userRepository.save(superAdmin);
        log.info("Super Admin registered: {}", request.getEmail());

        // Auto-login after registration
        return login(new LoginRequest(request.getEmail(), request.getPassword()));
    }

    /**
     * Get current authenticated user info.
     */
    @Transactional(readOnly = true)
    public UserDTO getCurrentUser(UserPrincipal userPrincipal) {
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userPrincipal.getId()));
        
        return mapToUserDTO(user);
    }

    /**
     * Change password for current user.
     */
    @Transactional
    public void changePassword(UserPrincipal userPrincipal, String currentPassword, String newPassword) {
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userPrincipal.getId()));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        log.info("Password changed for user: {}", user.getEmail());
    }

    private UserDTO mapToUserDTO(UserPrincipal principal) {
        return UserDTO.builder()
                .id(principal.getId())
                .email(principal.getEmail())
                .firstName(principal.getFirstName())
                .lastName(principal.getLastName())
                .role(principal.getRole().name())
                .organizationId(principal.getOrganizationId())
                .organizationSlug(principal.getOrganizationSlug())
                .isActive(principal.isEnabled())
                .build();
    }

    private UserDTO mapToUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .organizationId(user.getOrganization() != null ? user.getOrganization().getId() : null)
                .organizationSlug(user.getOrganization() != null ? user.getOrganization().getSlug() : null)
                .organizationName(user.getOrganization() != null ? user.getOrganization().getName() : null)
                .isActive(user.getIsActive())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
