package com.vedvix.syncledger.repository;

import com.vedvix.syncledger.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for RefreshToken entity operations.
 * Supports token rotation, revocation, and session management.
 * 
 * @author vedvix
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Find refresh token by its hash.
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Find all valid (non-revoked, non-expired) tokens for a user.
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user.id = :userId AND rt.revoked = false AND rt.expiresAt > :now")
    List<RefreshToken> findValidTokensByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /**
     * Find all tokens for a user (for session management).
     */
    List<RefreshToken> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find all active sessions for a user.
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user.id = :userId AND rt.revoked = false AND rt.expiresAt > :now ORDER BY rt.lastUsedAt DESC")
    List<RefreshToken> findActiveSessionsByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /**
     * Find all tokens in a token family.
     */
    List<RefreshToken> findByTokenFamily(String tokenFamily);

    /**
     * Revoke all tokens in a token family (for token reuse attack detection).
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :now, rt.revocationReason = :reason WHERE rt.tokenFamily = :family")
    int revokeTokenFamily(@Param("family") String family, @Param("now") LocalDateTime now, @Param("reason") String reason);

    /**
     * Revoke all tokens for a user (logout from all devices).
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :now, rt.revocationReason = 'Logout all devices' WHERE rt.user.id = :userId AND rt.revoked = false")
    int revokeAllUserTokens(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /**
     * Revoke a specific token by ID.
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :now, rt.revocationReason = :reason WHERE rt.id = :tokenId")
    int revokeToken(@Param("tokenId") Long tokenId, @Param("now") LocalDateTime now, @Param("reason") String reason);

    /**
     * Delete expired tokens (for cleanup).
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :expiredBefore")
    int deleteExpiredTokens(@Param("expiredBefore") LocalDateTime expiredBefore);

    /**
     * Delete revoked tokens older than specified date (for cleanup).
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.revoked = true AND rt.revokedAt < :revokedBefore")
    int deleteOldRevokedTokens(@Param("revokedBefore") LocalDateTime revokedBefore);

    /**
     * Count active sessions for a user.
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user.id = :userId AND rt.revoked = false AND rt.expiresAt > :now")
    long countActiveSessionsByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /**
     * Check if a token family has any revoked tokens (for reuse detection).
     */
    @Query("SELECT COUNT(rt) > 0 FROM RefreshToken rt WHERE rt.tokenFamily = :family AND rt.revoked = true")
    boolean isTokenFamilyCompromised(@Param("family") String family);

    /**
     * Update last used timestamp.
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.lastUsedAt = :now WHERE rt.id = :tokenId")
    int updateLastUsed(@Param("tokenId") Long tokenId, @Param("now") LocalDateTime now);
}
