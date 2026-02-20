package com.vedvix.syncledger.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * RefreshToken entity for secure session management.
 * Implements token rotation and revocation for production-grade security.
 * 
 * Features:
 * - Server-side token storage for revocation capability
 * - Token family tracking to detect token reuse attacks
 * - Device/session tracking for user visibility
 * - Automatic cleanup of expired tokens
 * 
 * @author vedvix
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_refresh_token_hash", columnList = "tokenHash"),
    @Index(name = "idx_refresh_token_user", columnList = "user_id"),
    @Index(name = "idx_refresh_token_family", columnList = "tokenFamily"),
    @Index(name = "idx_refresh_token_expiry", columnList = "expiresAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Hash of the refresh token (not the token itself for security).
     * Using SHA-256 hash to prevent token exposure if database is compromised.
     */
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    /**
     * Token family ID for detecting token reuse attacks.
     * All tokens in a rotation chain share the same family ID.
     * If a token from the same family is used twice, the family is invalidated.
     */
    @Column(nullable = false, length = 36)
    private String tokenFamily;

    /**
     * User this token belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * When this token expires.
     */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Device information for session management.
     */
    @Column(length = 500)
    private String userAgent;

    /**
     * IP address of the client.
     */
    @Column(length = 45)
    private String ipAddress;

    /**
     * Device name (parsed from user agent or provided by client).
     */
    @Column(length = 100)
    private String deviceName;

    /**
     * Location information (derived from IP).
     */
    @Column(length = 100)
    private String location;

    /**
     * Whether this token is revoked.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean revoked = false;

    /**
     * When this token was revoked.
     */
    @Column
    private LocalDateTime revokedAt;

    /**
     * Reason for revocation.
     */
    @Column(length = 100)
    private String revocationReason;

    /**
     * When this token was created.
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * When this token was last used (for activity tracking).
     */
    @Column
    private LocalDateTime lastUsedAt;

    /**
     * Check if this token is expired.
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Check if this token is valid (not expired and not revoked).
     */
    public boolean isValid() {
        return !revoked && !isExpired();
    }

    /**
     * Revoke this token.
     */
    public void revoke(String reason) {
        this.revoked = true;
        this.revokedAt = LocalDateTime.now();
        this.revocationReason = reason;
    }
}
