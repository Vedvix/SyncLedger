package com.vedvix.syncledger.dto;

import lombok.*;

/**
 * Request DTO for an Org Admin to update their own ERP integration settings.
 *
 * @author vedvix
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateErpConfigRequest {

    private String erpType;        // NONE, SAGE, NETSUITE, ORACLE, QUICKBOOKS, SAP, XERO, CUSTOM
    private String erpApiEndpoint;
    private String erpApiKey;
    private String erpTenantId;
    private String erpCompanyId;
    private Boolean erpAutoSync;
}
