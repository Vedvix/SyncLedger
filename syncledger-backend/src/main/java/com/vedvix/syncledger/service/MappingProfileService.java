package com.vedvix.syncledger.service;

import com.vedvix.syncledger.dto.MappingProfileDTO;
import com.vedvix.syncledger.dto.MappingProfileRequest;
import com.vedvix.syncledger.exception.BadRequestException;
import com.vedvix.syncledger.exception.ResourceNotFoundException;
import com.vedvix.syncledger.model.MappingProfile;
import com.vedvix.syncledger.model.Organization;
import com.vedvix.syncledger.repository.MappingProfileRepository;
import com.vedvix.syncledger.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing organization-scoped mapping profiles.
 *
 * @author vedvix
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MappingProfileService {

    private final MappingProfileRepository mappingProfileRepository;
    private final OrganizationRepository organizationRepository;

    /**
     * Get all profiles for an organization.
     */
    @Transactional(readOnly = true)
    public List<MappingProfileDTO> getProfilesByOrganization(Long organizationId) {
        return mappingProfileRepository.findByOrganizationIdOrderByIsDefaultDescNameAsc(organizationId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get a single profile by ID, verifying organization access.
     */
    @Transactional(readOnly = true)
    public MappingProfileDTO getProfile(String profileId, Long organizationId) {
        MappingProfile profile = mappingProfileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("MappingProfile", "id", profileId));

        if (profile.getOrganization() != null && !profile.getOrganization().getId().equals(organizationId)) {
            throw new ResourceNotFoundException("MappingProfile", "id", profileId);
        }

        return mapToDTO(profile);
    }

    /**
     * Create a new mapping profile for an organization.
     */
    @Transactional
    public MappingProfileDTO createProfile(Long organizationId, MappingProfileRequest request, Long userId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));

        // Check for duplicate name
        if (mappingProfileRepository.existsByOrganizationIdAndName(organizationId, request.getName())) {
            throw new BadRequestException("A profile with name '" + request.getName() + "' already exists");
        }

        MappingProfile profile = MappingProfile.builder()
                .id(UUID.randomUUID().toString())
                .organization(org)
                .name(request.getName())
                .description(request.getDescription())
                .vendorPattern(request.getVendorPattern())
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .isBuiltin(false)
                .erpType(request.getErpType())
                .rulesJson(request.getRulesJson())
                .createdBy(userId)
                .build();

        // If this profile is set as default, unset other defaults
        if (Boolean.TRUE.equals(profile.getIsDefault())) {
            unsetDefaultProfiles(organizationId);
        }

        profile = mappingProfileRepository.save(profile);
        log.info("Created mapping profile '{}' for org {} by user {}", profile.getName(), organizationId, userId);

        return mapToDTO(profile);
    }

    /**
     * Update an existing mapping profile.
     */
    @Transactional
    public MappingProfileDTO updateProfile(String profileId, Long organizationId, MappingProfileRequest request) {
        MappingProfile profile = mappingProfileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("MappingProfile", "id", profileId));

        if (profile.getOrganization() != null && !profile.getOrganization().getId().equals(organizationId)) {
            throw new ResourceNotFoundException("MappingProfile", "id", profileId);
        }

        if (Boolean.TRUE.equals(profile.getIsBuiltin())) {
            throw new BadRequestException("Cannot modify built-in profiles");
        }

        // Check name uniqueness if changed
        if (!profile.getName().equals(request.getName())
                && mappingProfileRepository.existsByOrganizationIdAndName(organizationId, request.getName())) {
            throw new BadRequestException("A profile with name '" + request.getName() + "' already exists");
        }

        profile.setName(request.getName());
        profile.setDescription(request.getDescription());
        profile.setVendorPattern(request.getVendorPattern());
        profile.setErpType(request.getErpType());
        profile.setRulesJson(request.getRulesJson());

        if (request.getIsDefault() != null) {
            if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(profile.getIsDefault())) {
                unsetDefaultProfiles(organizationId);
            }
            profile.setIsDefault(request.getIsDefault());
        }

        profile = mappingProfileRepository.save(profile);
        log.info("Updated mapping profile '{}' (id={}) for org {}", profile.getName(), profileId, organizationId);

        return mapToDTO(profile);
    }

    /**
     * Delete a mapping profile.
     */
    @Transactional
    public void deleteProfile(String profileId, Long organizationId) {
        MappingProfile profile = mappingProfileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("MappingProfile", "id", profileId));

        if (profile.getOrganization() != null && !profile.getOrganization().getId().equals(organizationId)) {
            throw new ResourceNotFoundException("MappingProfile", "id", profileId);
        }

        if (Boolean.TRUE.equals(profile.getIsBuiltin())) {
            throw new BadRequestException("Cannot delete built-in profiles");
        }

        mappingProfileRepository.delete(profile);
        log.info("Deleted mapping profile '{}' (id={}) from org {}", profile.getName(), profileId, organizationId);
    }

    /**
     * Get the default profile for an organization (or null if none set).
     */
    @Transactional(readOnly = true)
    public MappingProfileDTO getDefaultProfile(Long organizationId) {
        return mappingProfileRepository.findByOrganizationIdAndIsDefaultTrue(organizationId)
                .map(this::mapToDTO)
                .orElse(null);
    }

    /**
     * Unset default flag for all profiles of an organization.
     */
    private void unsetDefaultProfiles(Long organizationId) {
        mappingProfileRepository.findByOrganizationIdAndIsDefaultTrue(organizationId)
                .ifPresent(p -> {
                    p.setIsDefault(false);
                    mappingProfileRepository.save(p);
                });
    }

    private MappingProfileDTO mapToDTO(MappingProfile profile) {
        return MappingProfileDTO.builder()
                .id(profile.getId())
                .organizationId(profile.getOrganization() != null ? profile.getOrganization().getId() : null)
                .organizationName(profile.getOrganization() != null ? profile.getOrganization().getName() : null)
                .name(profile.getName())
                .description(profile.getDescription())
                .vendorPattern(profile.getVendorPattern())
                .isDefault(profile.getIsDefault())
                .isBuiltin(profile.getIsBuiltin())
                .erpType(profile.getErpType())
                .rulesJson(profile.getRulesJson())
                .createdBy(profile.getCreatedBy())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
