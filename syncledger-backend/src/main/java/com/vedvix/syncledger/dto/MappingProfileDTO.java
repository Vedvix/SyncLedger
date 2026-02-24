package com.vedvix.syncledger.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for mapping profile responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MappingProfileDTO {
    private String id;
    private Long organizationId;
    private String organizationName;
    private String name;
    private String description;
    private String vendorPattern;
    private Boolean isDefault;
    private Boolean isBuiltin;
    private String erpType;
    private String rulesJson;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
