package com.vedvix.syncledger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Request DTO for creating/updating a mapping profile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MappingProfileRequest {

    @NotBlank(message = "Profile name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    private String description;

    @Size(max = 500, message = "Vendor pattern must not exceed 500 characters")
    private String vendorPattern;

    private Boolean isDefault;

    private String erpType;

    @NotBlank(message = "Rules JSON is required")
    private String rulesJson;
}
