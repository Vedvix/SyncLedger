package com.vedvix.syncledger.dto;

import com.vedvix.syncledger.service.erp.ErpPropertyDefinitions;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * DTO for ERP integration configuration display.
 * Properties are generic key-value pairs — secrets are masked.
 * propertyDefinitions tells the frontend what fields to render.
 *
 * @author vedvix
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpConfigDTO {

    private String erpType;
    private String erpTypeDisplayName;
    private Boolean erpConfigured;

    /** Current property values (secrets masked). */
    private Map<String, String> properties;

    /** Schema for the frontend to build a dynamic form. */
    private List<ErpPropertyDefinitions.PropertyDef> propertyDefinitions;
}
