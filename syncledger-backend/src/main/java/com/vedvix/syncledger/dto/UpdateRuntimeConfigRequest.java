package com.vedvix.syncledger.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRuntimeConfigRequest {
    @NotBlank(message = "Config value is required")
    private String value;
}
