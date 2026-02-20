package com.vedvix.syncledger.service;

import com.vedvix.syncledger.dto.SessionDTO;
import com.vedvix.syncledger.exception.UnauthorizedException;
import com.vedvix.syncledger.model.RefreshToken;
import com.vedvix.syncledger.model.User;
import com.vedvix.syncledger.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing refresh tokens with industry-standard security practices.
 * 
 * Features:
 * - Cryptographically secure token generation
 * - Token rotation on each refresh (new token issued, old one invalidated)
 * - Token family tracking for reuse attack detection
 * - Server-side revocation capability
 * - Device/session management
 * - Automatic cleanup of expired tokens
 * 
 * @author vedvix
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpirationMs;

    @Value("${session.max-active:10}")
    private int maxActiveSessions;

    private static final SecureRandom secureRandom = new SecureRandom();

    /**
     * Create a new refresh token for a user.
     * 
     * @param user The user to create the token for
     * @param userAgent The user agent string from the request
     * @param ipAddress The IP address of the client
     * @return The raw refresh token (to be sent to client)
     */
    @Transactional
    public String createRefreshToken(User user, String userAgent, String ipAddress) {
        // Generate a new token family for this login session
        String tokenFamily = UUID.randomUUID().toString();
        return createRefreshTokenWithFamily(user, tokenFamily, userAgent, ipAddress);
    }

    /**
     * Create a new refresh token with a specific family (for rotation).
     */
    @Transactional
    public String createRefreshTokenWithFamily(User user, String tokenFamily, String userAgent, String ipAddress) {
        // Check if user has too many active sessions, remove oldest
        enforceMaxSessions(user.getId());

        // Generate cryptographically secure token
        String rawToken = generateSecureToken();
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(tokenHash)
                .tokenFamily(tokenFamily)
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000))
                .userAgent(truncate(userAgent, 500))
                .ipAddress(truncate(ipAddress, 45))
                .deviceName(parseDeviceName(userAgent))
                .lastUsedAt(LocalDateTime.now())
                .build();

        refreshTokenRepository.save(refreshToken);

        log.debug("Created refresh token for user {} with family {}", user.getId(), tokenFamily);

        return rawToken;
    }

    /**
     * Rotate a refresh token - validate the old one, revoke it, create a new one.
     * This implements token rotation for enhanced security.
     * 
     * @param rawToken The refresh token to rotate
     * @param userAgent The user agent string from the request
     * @param ipAddress The IP address of the client
     * @return Object array containing [newRawToken, user]
     */
    @Transactional
    public Object[] rotateRefreshToken(String rawToken, String userAgent, String ipAddress) {
        String tokenHash = hashToken(rawToken);

        RefreshToken existingToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        // Check if token family is compromised (reuse attack detection)
        if (refreshTokenRepository.isTokenFamilyCompromised(existingToken.getTokenFamily())) {
            // Token family was already used! This indicates a token reuse attack.
            // Revoke ALL tokens in this family for security
            log.warn("Token reuse detected for user {}! Revoking entire token family: {}", 
                    existingToken.getUser().getId(), existingToken.getTokenFamily());
            
            refreshTokenRepository.revokeTokenFamily(
                    existingToken.getTokenFamily(), 
                    LocalDateTime.now(), 
                    "Token reuse attack detected"
            );
            
            throw new UnauthorizedException("Session invalidated due to security concern. Please login again.");
        }

        // Check if token is already revoked
        if (existingToken.getRevoked()) {
            log.warn("Attempted use of revoked token for user {}", existingToken.getUser().getId());
            throw new UnauthorizedException("Refresh token has been revoked");
        }

        // Check if token is expired
        if (existingToken.isExpired()) {
            log.debug("Attempted use of expired token for user {}", existingToken.getUser().getId());
            throw new UnauthorizedException("Refresh token has expired");
        }

        // Check if user is still active
        User user = existingToken.getUser();
        if (!user.getIsActive()) {
            throw new UnauthorizedException("User account is disabled");
        }

        // Revoke the old token (rotation)
        existingToken.revoke("Token rotated");
        refreshTokenRepository.save(existingToken);

        // Create new token in the same family
        String newRawToken = createRefreshTokenWithFamily(
                user, 
                existingToken.getTokenFamily(), 
                userAgent, 
                ipAddress
        );

        log.debug("Rotated refresh token for user {} in family {}", user.getId(), existingToken.getTokenFamily());

        return new Object[]{newRawToken, user};
    }

    /**
     * Validate a refresh token without rotation.
     */
    @Transactional(readOnly = true)
    public User validateRefreshToken(String rawToken) {
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (!refreshToken.isValid()) {
            throw new UnauthorizedException("Refresh token is invalid or expired");
        }

        return refreshToken.getUser();
    }

    /**
     * Revoke a specific refresh token (logout from one device).
     */
    @Transactional
    public void revokeToken(String rawToken) {
        String tokenHash = hashToken(rawToken);
        
        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(token -> {
                    token.revoke("User logout");
                    refreshTokenRepository.save(token);
                    log.debug("Revoked refresh token for user {}", token.getUser().getId());
                });
    }

    /**
     * Revoke a specific session by ID.
     */
    @Transactional
    public void revokeSession(Long userId, Long sessionId) {
        RefreshToken token = refreshTokenRepository.findById(sessionId)
                .orElseThrow(() -> new UnauthorizedException("Session not found"));

        // Ensure the session belongs to the user
        if (!token.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Unauthorized to revoke this session");
        }

        token.revoke("User revoked session");
        refreshTokenRepository.save(token);
        log.info("User {} revoked session {}", userId, sessionId);
    }

    /**
     * Revoke all refresh tokens for a user (logout from all devices).
     */
    @Transactional
    public int revokeAllUserTokens(Long userId) {
        int count = refreshTokenRepository.revokeAllUserTokens(userId, LocalDateTime.now());
        log.info("Revoked {} tokens for user {}", count, userId);
        return count;
    }

    /**
     * Get all active sessions for a user (for session management UI).
     */
    @Transactional(readOnly = true)
    public List<SessionDTO> getActiveSessions(Long userId) {
        List<RefreshToken> tokens = refreshTokenRepository.findActiveSessionsByUserId(
                userId, LocalDateTime.now());

        return tokens.stream()
                .map(this::toSessionDTO)
                .toList();
    }

    /**
     * Get session count for a user.
     */
    @Transactional(readOnly = true)
    public long getActiveSessionCount(Long userId) {
        return refreshTokenRepository.countActiveSessionsByUserId(userId, LocalDateTime.now());
    }

    /**
     * Cleanup expired and old revoked tokens.
     * Scheduled to run daily at 2 AM.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        // Delete tokens expired more than 7 days ago
        LocalDateTime expiredBefore = LocalDateTime.now().minusDays(7);
        int deletedExpired = refreshTokenRepository.deleteExpiredTokens(expiredBefore);

        // Delete revoked tokens older than 30 days
        LocalDateTime revokedBefore = LocalDateTime.now().minusDays(30);
        int deletedRevoked = refreshTokenRepository.deleteOldRevokedTokens(revokedBefore);

        log.info("Cleaned up {} expired and {} old revoked tokens", deletedExpired, deletedRevoked);
    }

    // ================================
    // Private Helper Methods
    // ================================

    private void enforceMaxSessions(Long userId) {
        List<RefreshToken> activeSessions = refreshTokenRepository.findActiveSessionsByUserId(
                userId, LocalDateTime.now());

        if (activeSessions.size() >= maxActiveSessions) {
            // Revoke the oldest sessions to make room
            int sessionsToRevoke = activeSessions.size() - maxActiveSessions + 1;
            for (int i = activeSessions.size() - 1; i >= activeSessions.size() - sessionsToRevoke; i--) {
                activeSessions.get(i).revoke("Max sessions exceeded");
                refreshTokenRepository.save(activeSessions.get(i));
            }
            log.debug("Revoked {} oldest sessions for user {} (max sessions: {})", 
                    sessionsToRevoke, userId, maxActiveSessions);
        }
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private String parseDeviceName(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown Device";
        }

        // Simple device detection based on user agent
        String ua = userAgent.toLowerCase();
        
        if (ua.contains("iphone")) return "iPhone";
        if (ua.contains("ipad")) return "iPad";
        if (ua.contains("android")) {
            if (ua.contains("mobile")) return "Android Phone";
            return "Android Tablet";
        }
        if (ua.contains("mac os")) return "Mac";
        if (ua.contains("windows")) return "Windows PC";
        if (ua.contains("linux")) return "Linux";
        if (ua.contains("chrome")) return "Chrome Browser";
        if (ua.contains("firefox")) return "Firefox Browser";
        if (ua.contains("safari")) return "Safari Browser";
        
        return "Web Browser";
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    private SessionDTO toSessionDTO(RefreshToken token) {
        return SessionDTO.builder()
                .id(token.getId())
                .deviceName(token.getDeviceName())
                .ipAddress(token.getIpAddress())
                .location(token.getLocation())
                .userAgent(token.getUserAgent())
                .createdAt(token.getCreatedAt())
                .lastUsedAt(token.getLastUsedAt())
                .expiresAt(token.getExpiresAt())
                .isCurrent(false) // Will be set by controller
                .build();
    }
}
