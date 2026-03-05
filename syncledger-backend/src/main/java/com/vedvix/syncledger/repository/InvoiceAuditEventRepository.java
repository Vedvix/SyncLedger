package com.vedvix.syncledger.repository;

import com.vedvix.syncledger.model.InvoiceAuditEvent;
import com.vedvix.syncledger.model.InvoiceAuditEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for invoice audit trail events.
 *
 * @author vedvix
 */
@Repository
public interface InvoiceAuditEventRepository extends JpaRepository<InvoiceAuditEvent, Long> {

    /**
     * Get the full audit trail for an invoice, ordered chronologically.
     */
    List<InvoiceAuditEvent> findByInvoiceIdOrderByCreatedAtAsc(Long invoiceId);

    /**
     * Get audit trail scoped by organization.
     */
    List<InvoiceAuditEvent> findByInvoiceIdAndOrganizationIdOrderByCreatedAtAsc(Long invoiceId, Long organizationId);

    /**
     * Count events of a specific type for an invoice.
     */
    long countByInvoiceIdAndEventType(Long invoiceId, InvoiceAuditEventType eventType);
}
