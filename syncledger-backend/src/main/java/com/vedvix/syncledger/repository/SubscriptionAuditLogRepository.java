package com.vedvix.syncledger.repository;

import com.vedvix.syncledger.model.SubscriptionAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for SubscriptionAuditLog entity.
 * 
 * @author vedvix
 */
@Repository
public interface SubscriptionAuditLogRepository extends JpaRepository<SubscriptionAuditLog, Long> {

    /**
     * Find audit logs by organization ID.
     */
    Page<SubscriptionAuditLog> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId, Pageable pageable);

    /**
     * Find audit logs by subscription ID.
     */
    List<SubscriptionAuditLog> findBySubscriptionIdOrderByCreatedAtDesc(Long subscriptionId);

    /**
     * Find audit logs by event type.
     */
    List<SubscriptionAuditLog> findByEventTypeOrderByCreatedAtDesc(String eventType);
}
