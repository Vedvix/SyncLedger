package com.vedvix.syncledger.repository;

import com.vedvix.syncledger.model.PlanDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for plan definitions.
 *
 * @author vedvix
 */
@Repository
public interface PlanDefinitionRepository extends JpaRepository<PlanDefinition, Long> {

    Optional<PlanDefinition> findByPlanKey(String planKey);

    List<PlanDefinition> findByActiveTrueOrderBySortOrderAsc();

    List<PlanDefinition> findAllByOrderBySortOrderAsc();

    boolean existsByPlanKey(String planKey);
}
