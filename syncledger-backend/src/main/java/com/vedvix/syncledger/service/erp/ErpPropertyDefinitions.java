package com.vedvix.syncledger.service.erp;

import com.vedvix.syncledger.model.ErpType;

import java.util.*;

/**
 * Defines the properties (credentials / settings) each ERP type requires.
 * The frontend calls GET /erp-types/{type}/properties to discover which fields
 * to render in the configuration form. Each property has metadata so the UI can
 * build a dynamic form with labels, help text, required markers, and secret masking.
 *
 * @author vedvix
 */
public final class ErpPropertyDefinitions {

    private ErpPropertyDefinitions() {}

    /**
     * Metadata for a single ERP property field.
     */
    public record PropertyDef(
            String key,
            String label,
            String helpText,
            String type,        // "text", "password", "url", "select"
            boolean required,
            boolean secret,     // encrypted at rest, masked in responses
            String defaultValue,
            int displayOrder
    ) {}

    private static final Map<ErpType, List<PropertyDef>> DEFINITIONS = new LinkedHashMap<>();

    static {
        // ─── Sage Intacct (XML Web Services) ────────────────────────────────
        DEFINITIONS.put(ErpType.SAGE, List.of(
                new PropertyDef("sender_id", "Sender ID",
                        "Web Services developer Sender ID. Register at https://developer.intacct.com",
                        "text", true, false, null, 1),
                new PropertyDef("sender_password", "Sender Password",
                        "Paired password for the Sender ID",
                        "password", true, true, null, 2),
                new PropertyDef("company_id", "Company ID",
                        "Sage Intacct Company ID provided by the customer",
                        "text", true, false, null, 3),
                new PropertyDef("user_id", "User ID",
                        "Web Services User ID created in the customer's Sage Intacct instance",
                        "text", true, false, null, 4),
                new PropertyDef("user_password", "User Password",
                        "Password for the Web Services User",
                        "password", true, true, null, 5),
                new PropertyDef("gateway_url", "API Gateway URL",
                        "XML gateway endpoint. Leave blank for default.",
                        "url", false, false, "https://api.intacct.com/ia/xml/xmlgw.phtml", 6),
                new PropertyDef("entity_id", "Entity ID",
                        "Optional entity/location ID for multi-entity companies",
                        "text", false, false, null, 7),
                new PropertyDef("auto_sync", "Auto-Sync on Approval",
                        "Automatically sync invoices when approved",
                        "select", false, false, "true", 8)
        ));

        // ─── QuickBooks Online (OAuth2 REST) ────────────────────────────────
        DEFINITIONS.put(ErpType.QUICKBOOKS, List.of(
                new PropertyDef("client_id", "Client ID",
                        "OAuth2 Client ID from Intuit Developer Portal (https://developer.intuit.com)",
                        "text", true, false, null, 1),
                new PropertyDef("client_secret", "Client Secret",
                        "OAuth2 Client Secret",
                        "password", true, true, null, 2),
                new PropertyDef("realm_id", "Realm ID (Company ID)",
                        "QuickBooks company ID — found in the URL when logged into QuickBooks Online",
                        "text", true, false, null, 3),
                new PropertyDef("refresh_token", "Refresh Token",
                        "OAuth2 refresh token obtained after authorization flow",
                        "password", true, true, null, 4),
                new PropertyDef("environment", "Environment",
                        "sandbox or production",
                        "select", false, false, "production", 5),
                new PropertyDef("auto_sync", "Auto-Sync on Approval",
                        "Automatically sync invoices when approved",
                        "select", false, false, "true", 6)
        ));

        // ─── Oracle NetSuite (Token-Based Auth REST) ────────────────────────
        DEFINITIONS.put(ErpType.NETSUITE, List.of(
                new PropertyDef("account_id", "Account ID",
                        "NetSuite account ID (e.g. 1234567 or TSTDRV1234567 for sandbox)",
                        "text", true, false, null, 1),
                new PropertyDef("consumer_key", "Consumer Key",
                        "Integration record Consumer Key from Setup → Integration → Manage Integrations",
                        "text", true, false, null, 2),
                new PropertyDef("consumer_secret", "Consumer Secret",
                        "Integration record Consumer Secret",
                        "password", true, true, null, 3),
                new PropertyDef("token_id", "Token ID",
                        "Token-based auth Token ID from Setup → Users/Roles → Access Tokens",
                        "text", true, false, null, 4),
                new PropertyDef("token_secret", "Token Secret",
                        "Token-based auth Token Secret",
                        "password", true, true, null, 5),
                new PropertyDef("base_url", "Base URL",
                        "NetSuite REST API base URL. Leave blank for default.",
                        "url", false, false, null, 6),
                new PropertyDef("auto_sync", "Auto-Sync on Approval",
                        "Automatically sync invoices when approved",
                        "select", false, false, "true", 7)
        ));

        // ─── Oracle Fusion Cloud ERP (OAuth2 REST) ──────────────────────────
        DEFINITIONS.put(ErpType.ORACLE, List.of(
                new PropertyDef("base_url", "Cloud Instance URL",
                        "Oracle Fusion Cloud base URL (e.g. https://abcd-test.fa.us2.oraclecloud.com)",
                        "url", true, false, null, 1),
                new PropertyDef("username", "Username",
                        "Integration user with AP access in Oracle Fusion",
                        "text", true, false, null, 2),
                new PropertyDef("password", "Password",
                        "Password for the integration user",
                        "password", true, true, null, 3),
                new PropertyDef("business_unit", "Business Unit",
                        "Oracle Fusion Business Unit for invoice creation",
                        "text", false, false, null, 4),
                new PropertyDef("auto_sync", "Auto-Sync on Approval",
                        "Automatically sync invoices when approved",
                        "select", false, false, "true", 5)
        ));

        // ─── SAP (S/4HANA or Business One) ──────────────────────────────────
        DEFINITIONS.put(ErpType.SAP, List.of(
                new PropertyDef("base_url", "SAP Server URL",
                        "SAP S/4HANA OData API or SAP Business One Service Layer URL",
                        "url", true, false, null, 1),
                new PropertyDef("client_id", "Client ID / API Key",
                        "SAP API Business Hub API key or S/4HANA OAuth client ID",
                        "text", true, false, null, 2),
                new PropertyDef("client_secret", "Client Secret",
                        "OAuth client secret (for S/4HANA Cloud) or password (for Business One)",
                        "password", true, true, null, 3),
                new PropertyDef("company_db", "Company Database",
                        "SAP Business One company database name (for B1 only)",
                        "text", false, false, null, 4),
                new PropertyDef("username", "Username",
                        "SAP login username",
                        "text", true, false, null, 5),
                new PropertyDef("password", "Password",
                        "SAP login password",
                        "password", true, true, null, 6),
                new PropertyDef("auto_sync", "Auto-Sync on Approval",
                        "Automatically sync invoices when approved",
                        "select", false, false, "true", 7)
        ));

        // ─── Xero (OAuth2 REST) ─────────────────────────────────────────────
        DEFINITIONS.put(ErpType.XERO, List.of(
                new PropertyDef("client_id", "Client ID",
                        "OAuth2 Client ID from Xero Developer Portal (https://developer.xero.com)",
                        "text", true, false, null, 1),
                new PropertyDef("client_secret", "Client Secret",
                        "OAuth2 Client Secret",
                        "password", true, true, null, 2),
                new PropertyDef("tenant_id", "Tenant ID (Organization ID)",
                        "Xero organization tenant ID obtained after authorization",
                        "text", true, false, null, 3),
                new PropertyDef("refresh_token", "Refresh Token",
                        "OAuth2 refresh token obtained after authorization flow",
                        "password", true, true, null, 4),
                new PropertyDef("auto_sync", "Auto-Sync on Approval",
                        "Automatically sync invoices when approved",
                        "select", false, false, "true", 5)
        ));

        // ─── Custom API ─────────────────────────────────────────────────────
        DEFINITIONS.put(ErpType.CUSTOM, List.of(
                new PropertyDef("base_url", "API Base URL",
                        "The base URL of your custom ERP API",
                        "url", true, false, null, 1),
                new PropertyDef("auth_type", "Authentication Type",
                        "bearer_token, api_key, basic, or oauth2",
                        "select", true, false, "bearer_token", 2),
                new PropertyDef("api_key", "API Key / Bearer Token",
                        "API key, bearer token, or OAuth2 client ID",
                        "password", true, true, null, 3),
                new PropertyDef("api_secret", "API Secret",
                        "API secret or OAuth2 client secret (if applicable)",
                        "password", false, true, null, 4),
                new PropertyDef("username", "Username",
                        "Username for basic auth (if applicable)",
                        "text", false, false, null, 5),
                new PropertyDef("password", "Password",
                        "Password for basic auth (if applicable)",
                        "password", false, true, null, 6),
                new PropertyDef("create_bill_path", "Create Bill API Path",
                        "Relative path for creating AP bills (e.g. /api/v1/bills)",
                        "text", false, false, "/api/v1/bills", 7),
                new PropertyDef("auto_sync", "Auto-Sync on Approval",
                        "Automatically sync invoices when approved",
                        "select", false, false, "true", 8)
        ));
    }

    /**
     * Get property definitions for an ERP type. Returns empty list for NONE.
     */
    public static List<PropertyDef> getDefinitions(ErpType erpType) {
        return DEFINITIONS.getOrDefault(erpType, List.of());
    }

    /**
     * Get all supported ERP types with their property counts.
     */
    public static Map<ErpType, List<PropertyDef>> getAllDefinitions() {
        return Collections.unmodifiableMap(DEFINITIONS);
    }

    /**
     * Check if a property key is marked as secret for a given ERP type.
     */
    public static boolean isSecret(ErpType erpType, String propertyKey) {
        return getDefinitions(erpType).stream()
                .filter(p -> p.key().equals(propertyKey))
                .findFirst()
                .map(PropertyDef::secret)
                .orElse(false);
    }

    /**
     * Get required property keys for an ERP type.
     */
    public static List<String> getRequiredKeys(ErpType erpType) {
        return getDefinitions(erpType).stream()
                .filter(PropertyDef::required)
                .map(PropertyDef::key)
                .toList();
    }
}
