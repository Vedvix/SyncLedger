package com.vedvix.syncledger.dto;

import lombok.*;

import java.util.Map;

/**
 * Request DTO for updating ERP integration settings.
 * Generic key-value properties — the required fields depend on the selected erpType.
 *
 * @author vedvix
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateErpConfigRequest {

    /** ERP type: NONE, SAGE, NETSUITE, ORACLE, QUICKBOOKS, SAP, XERO, CUSTOM */
    private String erpType;

    /** Key-value properties for the selected ERP type (secrets sent in plain text, encrypted at rest). */
    private Map<String, String> properties;
}
