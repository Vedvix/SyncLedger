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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Authentication service handling login, registration, and token management.
 * Supports multi-tenant authentication with organization context.
 * 
 * Features:
 * - Secure refresh token rotation
 * - Token reuse attack detection
 * - Session management (view/revoke sessions)
 * - Multi-device support with configurable limits
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
    private final RefreshTokenService refreshTokenService;

    @Value("${jwt.expiration:3600000}")
    private long accessTokenExpirationMs;

    /**
     * Authenticate user and generate JWT tokens.
     * Creates a server-side refresh token for session management.
     * 
     * @param request Login credentials
     * @param userAgent User agent string from request
     * @param ipAddress IP address of the client
     * @return Authentication response with tokens
     */
    @Transactional
    public AuthResponse login(LoginRequest request, String userAgent, String ipAddress) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getEmail(),
                    request.getPassword()
                )
            );

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            User user = userRepository.findById(userPrincipal.getId())
                    .orElseThrow(() -> new UnauthorizedException("User not found"));
            
            // Update last login timestamp (also resets failed attempts)
            userRepository.updateLastLogin(userPrincipal.getId(), LocalDateTime.now());
            
            // Generate access token (stateless JWT)
            String accessToken = tokenProvider.generateToken(userPrincipal);
            
            // Generate refresh token (stored server-side for revocation support)
            String refreshToken = refreshTokenService.createRefreshToken(user, userAgent, ipAddress);

            log.info("User {} logged in successfully from IP: {}", request.getEmail(), ipAddress);

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(accessTokenExpirationMs / 1000)
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
     * Legacy login method for backward compatibility.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        return login(request, null, null);
    }

    /**
     * Refresh access token using refresh token.
     * Implements token rotation - the old refresh token is invalidated
     * and a new one is issued.
     * 
     * @param refreshToken The refresh token to use
     * @param userAgent User agent string from request
     * @param ipAddress IP address of the client
     * @return New authentication response with rotated tokens
     */
    @Transactional
    public AuthResponse refreshToken(String refreshToken, String userAgent, String ipAddress) {
        // Rotate the refresh token (validates, revokes old, creates new)
        Object[] result = refreshTokenService.rotateRefreshToken(refreshToken, userAgent, ipAddress);
        String newRefreshToken = (String) result[0];
        User user = (User) result[1];

        if (!user.getIsActive()) {
            throw new UnauthorizedException("User account is disabled");
        }

        UserPrincipal userPrincipal = UserPrincipal.create(user);
        String newAccessToken = tokenProvider.generateToken(userPrincipal);

        log.debug("Token refreshed for user {} from IP: {}", user.getEmail(), ipAddress);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpirationMs / 1000)
                .user(mapToUserDTO(userPrincipal))
                .build();
    }

    /**
     * Legacy refresh token method for backward compatibility.
     */
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        return refreshToken(refreshToken, null, null);
    }

    /**
     * Logout user - revokes the current refresh token.
     * 
     * @param refreshToken The refresh token to revoke
     */
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isEmpty()) {
            refreshTokenService.revokeToken(refreshToken);
            log.debug("User logged out, refresh token revoked");
        }
    }

    /**
     * Logout from all devices - revokes all refresh tokens for a user.
     * 
     * @param userId The user ID
     * @return Number of sessions revoked
     */
    @Transactional
    public int logoutAllDevices(Long userId) {
        int count = refreshTokenService.revokeAllUserTokens(userId);
        log.info("Logged out user {} from {} devices", userId, count);
        return count;
    }

    /**
     * Get all active sessions for a user.
     * 
     * @param userId The user ID
     * @param currentRefreshToken The current refresh token (to mark as current)
     * @return List of active sessions
     */
    @Transactional(readOnly = true)
    public List<SessionDTO> getActiveSessions(Long userId, String currentRefreshToken) {
        List<SessionDTO> sessions = refreshTokenService.getActiveSessions(userId);
        
        // Mark the current session
        if (currentRefreshToken != null) {
            // We can't directly compare tokens since we only store hashes,
            // so we just return sessions as-is. The frontend will track current session.
        }
        
        return sessions;
    }

    /**
     * Revoke a specific session.
     * 
     * @param userId The user ID (for authorization check)
     * @param sessionId The session ID to revoke
     */
    @Transactional
    public void revokeSession(Long userId, Long sessionId) {
        refreshTokenService.revokeSession(userId, sessionId);
        log.info("User {} revoked session {}", userId, sessionId);
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
                .organizationStatus(principal.getOrganizationStatus())
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
                .organizationStatus(user.getOrganization() != null ? user.getOrganization().getStatus().name() : null)
                .isActive(user.getIsActive())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
