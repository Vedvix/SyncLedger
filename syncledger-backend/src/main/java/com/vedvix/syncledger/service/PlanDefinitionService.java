package com.vedvix.syncledger.service;

import com.vedvix.syncledger.dto.PlanDefinitionDTO;
import com.vedvix.syncledger.dto.PlanDefinitionRequest;
import com.vedvix.syncledger.exception.ResourceNotFoundException;
import com.vedvix.syncledger.model.PlanDefinition;
import com.vedvix.syncledger.repository.PlanDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing dynamic subscription plan definitions.
 *
 * @author vedvix
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanDefinitionService {

    private final PlanDefinitionRepository planRepo;

    // ==================== Public / Read ====================

    /**
     * Returns only active plans (for the pricing page).
     */
    @Transactional(readOnly = true)
    public List<PlanDefinitionDTO> getActivePlans() {
        return planRepo.findByActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Returns ALL plans including inactive (for Super Admin management).
     */
    @Transactional(readOnly = true)
    public List<PlanDefinitionDTO> getAllPlans() {
        return planRepo.findAllByOrderBySortOrderAsc()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlanDefinitionDTO getPlanById(Long id) {
        PlanDefinition plan = planRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PlanDefinition", "id", id));
        return toDTO(plan);
    }

    @Transactional(readOnly = true)
    public PlanDefinitionDTO getPlanByKey(String planKey) {
        PlanDefinition plan = planRepo.findByPlanKey(planKey)
                .orElseThrow(() -> new ResourceNotFoundException("PlanDefinition", "planKey", planKey));
        return toDTO(plan);
    }

    // ==================== Admin CRUD ====================

    @Transactional
    public PlanDefinitionDTO createPlan(PlanDefinitionRequest request) {
        if (planRepo.existsByPlanKey(request.getPlanKey().toUpperCase())) {
            throw new IllegalArgumentException("Plan key '" + request.getPlanKey() + "' already exists");
        }

        PlanDefinition plan = PlanDefinition.builder()
                .planKey(request.getPlanKey().toUpperCase())
                .displayName(request.getDisplayName())
                .description(request.getDescription())
                .monthlyPrice(request.getMonthlyPrice())
                .annualPrice(request.getAnnualPrice())
                .invoicesPerMonth(defaultStr(request.getInvoicesPerMonth(), "0"))
                .maxUsers(defaultStr(request.getMaxUsers(), "0"))
                .maxOrganizations(defaultStr(request.getMaxOrganizations(), "0"))
                .maxEmailInboxes(defaultStr(request.getMaxEmailInboxes(), "0"))
                .storage(defaultStr(request.getStorage(), "0"))
                .approvalType(defaultStr(request.getApprovalType(), "Basic"))
                .supportLevel(defaultStr(request.getSupportLevel(), "Email"))
                .uptimeSla(defaultStr(request.getUptimeSla(), "99.5%"))
                .highlight(request.getHighlight() != null ? request.getHighlight() : false)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        plan = planRepo.save(plan);
        log.info("Created plan definition: {} ({})", plan.getDisplayName(), plan.getPlanKey());
        return toDTO(plan);
    }

    @Transactional
    public PlanDefinitionDTO updatePlan(Long id, PlanDefinitionRequest request) {
        PlanDefinition plan = planRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PlanDefinition", "id", id));

        // If planKey changed, check for conflicts
        String newKey = request.getPlanKey().toUpperCase();
        if (!plan.getPlanKey().equals(newKey) && planRepo.existsByPlanKey(newKey)) {
            throw new IllegalArgumentException("Plan key '" + newKey + "' already exists");
        }

        plan.setPlanKey(newKey);
        plan.setDisplayName(request.getDisplayName());
        plan.setDescription(request.getDescription());
        plan.setMonthlyPrice(request.getMonthlyPrice());
        plan.setAnnualPrice(request.getAnnualPrice());

        if (request.getInvoicesPerMonth() != null) plan.setInvoicesPerMonth(request.getInvoicesPerMonth());
        if (request.getMaxUsers() != null)         plan.setMaxUsers(request.getMaxUsers());
        if (request.getMaxOrganizations() != null)  plan.setMaxOrganizations(request.getMaxOrganizations());
        if (request.getMaxEmailInboxes() != null)   plan.setMaxEmailInboxes(request.getMaxEmailInboxes());
        if (request.getStorage() != null)           plan.setStorage(request.getStorage());
        if (request.getApprovalType() != null)      plan.setApprovalType(request.getApprovalType());
        if (request.getSupportLevel() != null)      plan.setSupportLevel(request.getSupportLevel());
        if (request.getUptimeSla() != null)         plan.setUptimeSla(request.getUptimeSla());
        if (request.getHighlight() != null)         plan.setHighlight(request.getHighlight());
        if (request.getSortOrder() != null)         plan.setSortOrder(request.getSortOrder());
        if (request.getActive() != null)            plan.setActive(request.getActive());

        plan = planRepo.save(plan);
        log.info("Updated plan definition: {} ({})", plan.getDisplayName(), plan.getPlanKey());
        return toDTO(plan);
    }

    @Transactional
    public void deletePlan(Long id) {
        PlanDefinition plan = planRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PlanDefinition", "id", id));
        // Soft-delete: deactivate instead of removing
        plan.setActive(false);
        planRepo.save(plan);
        log.info("Deactivated plan definition: {} ({})", plan.getDisplayName(), plan.getPlanKey());
    }

    // ==================== Mapping ====================

    private PlanDefinitionDTO toDTO(PlanDefinition p) {
        return PlanDefinitionDTO.builder()
                .id(p.getId())
                .planKey(p.getPlanKey())
                .displayName(p.getDisplayName())
                .description(p.getDescription())
                .monthlyPrice(p.getMonthlyPrice())
                .annualPrice(p.getAnnualPrice())
                .invoicesPerMonth(p.getInvoicesPerMonth())
                .maxUsers(p.getMaxUsers())
                .maxOrganizations(p.getMaxOrganizations())
                .maxEmailInboxes(p.getMaxEmailInboxes())
                .storage(p.getStorage())
                .approvalType(p.getApprovalType())
                .supportLevel(p.getSupportLevel())
                .uptimeSla(p.getUptimeSla())
                .highlight(p.getHighlight())
                .sortOrder(p.getSortOrder())
                .active(p.getActive())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private String defaultStr(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }
}
