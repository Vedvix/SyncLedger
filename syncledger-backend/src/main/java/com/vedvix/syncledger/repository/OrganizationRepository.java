package com.vedvix.syncledger.repository;

import com.vedvix.syncledger.model.Organization;
import com.vedvix.syncledger.model.OrganizationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Organization entity.
 * Supports multi-tenant SaaS operations.
 * 
 * @author vedvix
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    /**
     * Find organization by slug (URL-friendly identifier).
     */
    Optional<Organization> findBySlug(String slug);

    /**
     * Find organization by email address.
     */
    Optional<Organization> findByEmailAddress(String emailAddress);

    /**
     * Check if slug exists.
     */
    boolean existsBySlug(String slug);

    /**
     * Check if email address exists.
     */
    boolean existsByEmailAddress(String emailAddress);

    /**
     * Find all organizations by status.
     */
    List<Organization> findByStatus(OrganizationStatus status);

    /**
     * Find all active organizations.
     */
    @Query("SELECT o FROM Organization o WHERE o.status = 'ACTIVE'")
    List<Organization> findAllActive();

    /**
     * Find organizations with pagination.
     */
    Page<Organization> findByStatus(OrganizationStatus status, Pageable pageable);

    /**
     * Search organizations by name.
     */
    @Query("SELECT o FROM Organization o WHERE LOWER(o.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Organization> searchByName(@Param("search") String search, Pageable pageable);

    /**
     * Count organizations by status.
     */
    long countByStatus(OrganizationStatus status);

    /**
     * Find organizations needing email sync (active with email configured).
     */
    @Query("SELECT o FROM Organization o WHERE o.status = 'ACTIVE' AND o.emailAddress IS NOT NULL")
    List<Organization> findOrganizationsForEmailSync();

    /**
     * Find organization by S3 folder path.
     */
    Optional<Organization> findByS3FolderPath(String s3FolderPath);
}
