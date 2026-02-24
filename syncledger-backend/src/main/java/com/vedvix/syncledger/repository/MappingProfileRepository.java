package com.vedvix.syncledger.repository;

import com.vedvix.syncledger.model.MappingProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for MappingProfile entity operations.
 *
 * @author vedvix
 */
@Repository
public interface MappingProfileRepository extends JpaRepository<MappingProfile, String> {

    /**
     * Find all profiles for an organization, ordered by default flag then name.
     */
    List<MappingProfile> findByOrganizationIdOrderByIsDefaultDescNameAsc(Long organizationId);

    /**
     * Find the default profile for an organization.
     */
    Optional<MappingProfile> findByOrganizationIdAndIsDefaultTrue(Long organizationId);

    /**
     * Find profiles by organization and ERP type.
     */
    List<MappingProfile> findByOrganizationIdAndErpType(Long organizationId, String erpType);

    /**
     * Find a profile by organization and name.
     */
    Optional<MappingProfile> findByOrganizationIdAndName(Long organizationId, String name);

    /**
     * Check if a profile name already exists for an organization.
     */
    boolean existsByOrganizationIdAndName(Long organizationId, String name);

    /**
     * Find all built-in profiles (shared across organizations).
     */
    List<MappingProfile> findByIsBuiltinTrue();

    /**
     * Count profiles for an organization.
     */
    long countByOrganizationId(Long organizationId);

    /**
     * Delete all profiles for an organization.
     */
    void deleteByOrganizationId(Long organizationId);

    /**
     * Find profiles matching a vendor pattern for an organization.
     */
    @Query("SELECT mp FROM MappingProfile mp WHERE mp.organization.id = :orgId " +
           "AND mp.vendorPattern IS NOT NULL AND :vendorName LIKE CONCAT('%', mp.vendorPattern, '%') " +
           "ORDER BY mp.isDefault DESC")
    List<MappingProfile> findMatchingProfiles(@Param("orgId") Long orgId, @Param("vendorName") String vendorName);
}
