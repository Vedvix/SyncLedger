package com.vedvix.syncledger.service.erp;

import com.vedvix.syncledger.model.ErpType;
import com.vedvix.syncledger.model.Invoice;

import java.util.Map;

/**
 * Generic ERP connector interface. Each ERP implementation (Sage Intacct, QuickBooks,
 * NetSuite, etc.) implements this interface. The connector reads its configuration
 * from a generic key-value property map rather than hardcoded Organization fields.
 *
 * @author vedvix
 */
public interface ErpConnector {

    /**
     * The ERP type this connector handles.
     */
    ErpType getErpType();

    /**
     * Test connectivity using the provided properties.
     *
     * @param properties decrypted key-value properties for this ERP
     * @return result of the connection test
     */
    ErpSyncResult testConnection(Map<String, String> properties);

    /**
     * Create an AP Bill / Purchase Invoice in the target ERP.
     *
     * @param invoice    the approved invoice to sync
     * @param properties decrypted key-value properties for this ERP
     * @return result including the remote record ID
     */
    ErpSyncResult createBill(Invoice invoice, Map<String, String> properties);
}
