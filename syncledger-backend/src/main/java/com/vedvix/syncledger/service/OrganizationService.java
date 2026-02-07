package com.vedvix.syncledger.service;

import com.vedvix.syncledger.dto.*;
import com.vedvix.syncledger.exception.BadRequestException;
import com.vedvix.syncledger.exception.ResourceNotFoundException;
import com.vedvix.syncledger.exception.UnauthorizedException;
import com.vedvix.syncledger.model.Organization;
import com.vedvix.syncledger.model.OrganizationStatus;
import com.vedvix.syncledger.repository.OrganizationRepository;
import com.vedvix.syncledger.repository.UserRepository;
import com.vedvix.syncledger.repository.InvoiceRepository;
import com.vedvix.syncledger.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing Organizations (multi-tenant).
 * Only accessible by Super Admin.
 * 
 * @author vedvix
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;

    /**
     * Create a new organization.
     */
    @Transactional
    public OrganizationDTO createOrganization(CreateOrganizationRequest request, UserPrincipal currentUser) {
        validateSuperAdmin(currentUser);

        // Check for duplicates
        if (organizationRepository.existsBySlug(request.getSlug())) {
            throw new BadRequestException("Organization slug already exists: " + request.getSlug());
        }
        if (request.getEmailAddress() != null && 
            organizationRepository.existsByEmailAddress(request.getEmailAddress())) {
            throw new BadRequestException("Email address already registered to another organization");
        }

        Organization org = Organization.builder()
                .name(request.getName())
                .slug(request.getSlug().toLowerCase())
                .emailAddress(request.getEmailAddress())
                .status(OrganizationStatus.ONBOARDING)
                .sageApiEndpoint(request.getSageApiEndpoint())
                .sageApiKey(request.getSageApiKey())
                .s3FolderPath(generateS3FolderPath(request.getSlug()))
                .sqsQueueName(generateSqsQueueName(request.getSlug()))
                .build();

        organizationRepository.save(org);
        log.info("Organization created: {} by Super Admin: {}", org.getName(), currentUser.getEmail());

        return mapToDTO(org);
    }

    /**
     * Get all organizations with pagination.
     */
    @Transactional(readOnly = true)
    public PagedResponse<OrganizationDTO> getAllOrganizations(Pageable pageable, UserPrincipal currentUser) {
        validateSuperAdmin(currentUser);

        Page<Organization> orgs = organizationRepository.findAll(pageable);
        Page<OrganizationDTO> orgDTOs = orgs.map(this::mapToDTO);
        return PagedResponse.from(orgDTOs);
    }

    /**
     * Get organization by ID.
     */
    @Transactional(readOnly = true)
    public OrganizationDTO getOrganizationById(Long id, UserPrincipal currentUser) {
        validateSuperAdmin(currentUser);

        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", id));

        return mapToDTO(org);
    }

    /**
     * Get organization by slug.
     */
    @Transactional(readOnly = true)
    public OrganizationDTO getOrganizationBySlug(String slug, UserPrincipal currentUser) {
        validateSuperAdmin(currentUser);

        Organization org = organizationRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "slug", slug));

        return mapToDTO(org);
    }

    /**
     * Update organization.
     */
    @Transactional
    public OrganizationDTO updateOrganization(Long id, UpdateOrganizationRequest request, UserPrincipal currentUser) {
        validateSuperAdmin(currentUser);

        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", id));

        if (request.getName() != null) {
            org.setName(request.getName());
        }
        if (request.getEmailAddress() != null) {
            if (!request.getEmailAddress().equals(org.getEmailAddress()) &&
                organizationRepository.existsByEmailAddress(request.getEmailAddress())) {
                throw new BadRequestException("Email address already registered to another organization");
            }
            org.setEmailAddress(request.getEmailAddress());
        }
        if (request.getStatus() != null) {
            org.setStatus(OrganizationStatus.valueOf(request.getStatus()));
        }
        if (request.getSageApiEndpoint() != null) {
            org.setSageApiEndpoint(request.getSageApiEndpoint());
        }
        if (request.getSageApiKey() != null) {
            org.setSageApiKey(request.getSageApiKey());
        }

        organizationRepository.save(org);
        log.info("Organization updated: {} by Super Admin: {}", org.getName(), currentUser.getEmail());

        return mapToDTO(org);
    }

    /**
     * Activate organization.
     */
    @Transactional
    public OrganizationDTO activateOrganization(Long id, UserPrincipal currentUser) {
        validateSuperAdmin(currentUser);

        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", id));

        org.setStatus(OrganizationStatus.ACTIVE);
        organizationRepository.save(org);
        
        log.info("Organization activated: {} by Super Admin: {}", org.getName(), currentUser.getEmail());
        return mapToDTO(org);
    }

    /**
     * Suspend organization.
     */
    @Transactional
    public OrganizationDTO suspendOrganization(Long id, UserPrincipal currentUser) {
        validateSuperAdmin(currentUser);

        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", id));

        org.setStatus(OrganizationStatus.SUSPENDED);
        organizationRepository.save(org);
        
        log.info("Organization suspended: {} by Super Admin: {}", org.getName(), currentUser.getEmail());
        return mapToDTO(org);
    }

    /**
     * Get organization statistics.
     */
    @Transactional(readOnly = true)
    public OrganizationStatsDTO getOrganizationStats(Long id, UserPrincipal currentUser) {
        validateSuperAdmin(currentUser);

        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", id));

        long userCount = userRepository.countByOrganization_Id(id);
        long activeUserCount = userRepository.countByOrganization_IdAndIsActiveTrue(id);
        long invoiceCount = invoiceRepository.countByOrganization_Id(id);

        return OrganizationStatsDTO.builder()
                .organizationId(id)
                .organizationName(org.getName())
                .totalUsers(userCount)
                .activeUsers(activeUserCount)
                .totalInvoices(invoiceCount)
                .status(org.getStatus().name())
                .createdAt(org.getCreatedAt())
                .build();
    }

    /**
     * Get platform-wide statistics for Super Admin dashboard.
     */
    @Transactional(readOnly = true)
    public PlatformStatsDTO getPlatformStats(UserPrincipal currentUser) {
        validateSuperAdmin(currentUser);

        long totalOrgs = organizationRepository.count();
        long activeOrgs = organizationRepository.countByStatus(OrganizationStatus.ACTIVE);
        long totalUsers = userRepository.count();
        long totalInvoices = invoiceRepository.count();

        return PlatformStatsDTO.builder()
                .totalOrganizations(totalOrgs)
                .activeOrganizations(activeOrgs)
                .totalUsers(totalUsers)
                .totalInvoices(totalInvoices)
                .build();
    }

    private void validateSuperAdmin(UserPrincipal user) {
        if (!user.isSuperAdmin()) {
            throw new UnauthorizedException("Only Super Admin can access this resource");
        }
    }

    private String generateS3FolderPath(String slug) {
        return "organizations/" + slug.toLowerCase() + "/invoices";
    }

    private String generateSqsQueueName(String slug) {
        return "syncledger-" + slug.toLowerCase() + "-queue";
    }

    private OrganizationDTO mapToDTO(Organization org) {
        return OrganizationDTO.builder()
                .id(org.getId())
                .name(org.getName())
                .slug(org.getSlug())
                .emailAddress(org.getEmailAddress())
                .status(org.getStatus().name())
                .sageApiEndpoint(org.getSageApiEndpoint())
                .s3FolderPath(org.getS3FolderPath())
                .sqsQueueName(org.getSqsQueueName())
                .createdAt(org.getCreatedAt())
                .updatedAt(org.getUpdatedAt())
                .build();
    }
}

