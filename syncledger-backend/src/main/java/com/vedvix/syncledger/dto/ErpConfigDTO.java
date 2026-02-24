package com.vedvix.syncledger.dto;

import lombok.*;

/**
 * DTO for ERP integration configuration display (API key masked).
 *
 * @author vedvix
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpConfigDTO {

    private String erpType;
    private String erpApiEndpoint;
    private String erpApiKeyMasked;
    private String erpTenantId;
    private String erpCompanyId;
    private Boolean erpAutoSync;
    private Boolean erpConfigured;
}
