package com.vedvix.syncledger.repository;

import com.vedvix.syncledger.model.InvoiceLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repository for InvoiceLineItem entity operations.
 * 
 * @author vedvix
 */
@Repository
public interface InvoiceLineItemRepository extends JpaRepository<InvoiceLineItem, Long> {

    /**
     * Find line items by invoice.
     */
    List<InvoiceLineItem> findByInvoiceIdOrderByLineNumber(Long invoiceId);

    /**
     * Delete line items by invoice.
     */
    void deleteByInvoiceId(Long invoiceId);

    /**
     * Sum line totals for an invoice.
     */
    @Query("SELECT COALESCE(SUM(li.lineTotal), 0) FROM InvoiceLineItem li WHERE li.invoice.id = :invoiceId")
    BigDecimal sumLineTotalsByInvoiceId(@Param("invoiceId") Long invoiceId);

    /**
     * Find line items by GL account.
     */
    List<InvoiceLineItem> findByGlAccountCode(String glAccountCode);

    /**
     * Find line items by cost center.
     */
    List<InvoiceLineItem> findByCostCenter(String costCenter);

    /**
     * Count line items for an invoice.
     */
    long countByInvoiceId(Long invoiceId);
}
