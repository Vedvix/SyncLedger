package com.vedvix.syncledger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuntimeConfigDTO {
    private Long id;
    private String configKey;
    private String configValue;
    private String defaultValue;
    private String description;
    private String category;
    private String dataType;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
