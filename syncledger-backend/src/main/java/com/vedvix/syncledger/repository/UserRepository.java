package com.vedvix.syncledger.repository;

import com.vedvix.syncledger.model.User;
import com.vedvix.syncledger.model.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for User entity operations.
 * 
 * @author vedvix
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by email address.
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by email (case insensitive).
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Check if email already exists.
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Find all users by role.
     */
    List<User> findByRole(UserRole role);

    /**
     * Find all active users.
     */
    List<User> findByIsActiveTrue();

    /**
     * Find active users by role.
     */
    List<User> findByRoleAndIsActiveTrue(UserRole role);

    /**
     * Find users by department.
     */
    List<User> findByDepartmentIgnoreCase(String department);

    /**
     * Search users by name or email.
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<User> searchUsers(@Param("query") String query, Pageable pageable);

    /**
     * Find user by password reset token.
     */
    Optional<User> findByPasswordResetToken(String token);

    /**
     * Find users who haven't logged in since given date.
     */
    @Query("SELECT u FROM User u WHERE u.lastLoginAt < :since OR u.lastLoginAt IS NULL")
    List<User> findInactiveUsersSince(@Param("since") LocalDateTime since);

    /**
     * Count users by role.
     */
    long countByRole(UserRole role);

    /**
     * Count active users.
     */
    long countByIsActiveTrue();

    /**
     * Update last login timestamp.
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :timestamp, u.failedLoginAttempts = 0 WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") Long userId, @Param("timestamp") LocalDateTime timestamp);

    /**
     * Increment failed login attempts.
     */
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = COALESCE(u.failedLoginAttempts, 0) + 1 WHERE u.id = :userId")
    void incrementFailedLoginAttempts(@Param("userId") Long userId);

    /**
     * Lock user account.
     */
    @Modifying
    @Query("UPDATE User u SET u.lockedUntil = :lockedUntil WHERE u.id = :userId")
    void lockAccount(@Param("userId") Long userId, @Param("lockedUntil") LocalDateTime lockedUntil);

    /**
     * Get all distinct departments.
     */
    @Query("SELECT DISTINCT u.department FROM User u WHERE u.department IS NOT NULL ORDER BY u.department")
    List<String> findAllDepartments();
}
