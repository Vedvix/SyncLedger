package com.vedvix.syncledger.model;

/**
 * Supported ERP system types for invoice sync.
 * Each organization configures their specific ERP integration.
 */
public enum ErpType {

    NONE("None", "No ERP integration configured"),
    SAGE("Sage Intacct", "Sage Intacct cloud ERP"),
    NETSUITE("NetSuite", "Oracle NetSuite ERP"),
    ORACLE("Oracle", "Oracle Fusion Cloud ERP"),
    QUICKBOOKS("QuickBooks", "Intuit QuickBooks"),
    SAP("SAP", "SAP S/4HANA or Business One"),
    XERO("Xero", "Xero accounting platform"),
    CUSTOM("Custom", "Custom API integration");

    private final String displayName;
    private final String description;

    ErpType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
