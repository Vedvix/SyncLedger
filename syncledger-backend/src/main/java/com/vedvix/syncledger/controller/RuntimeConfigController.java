package com.vedvix.syncledger.controller;

import com.vedvix.syncledger.dto.ApiResponseDto;
import com.vedvix.syncledger.dto.RuntimeConfigDTO;
import com.vedvix.syncledger.dto.UpdateRuntimeConfigRequest;
import com.vedvix.syncledger.model.RuntimeConfig;
import com.vedvix.syncledger.security.UserPrincipal;
import com.vedvix.syncledger.service.RuntimeConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/super-admin/runtime-config")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class RuntimeConfigController {

    private final RuntimeConfigService runtimeConfigService;

    @GetMapping
    public ResponseEntity<ApiResponseDto<List<RuntimeConfigDTO>>> getAllConfigs() {
        List<RuntimeConfigDTO> configs = runtimeConfigService.getAllConfigs().stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(ApiResponseDto.success(configs));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponseDto<List<RuntimeConfigDTO>>> getConfigsByCategory(
            @PathVariable String category) {
        List<RuntimeConfigDTO> configs = runtimeConfigService.getConfigsByCategory(category).stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(ApiResponseDto.success(configs));
    }

    @PutMapping("/{key}")
    public ResponseEntity<ApiResponseDto<RuntimeConfigDTO>> updateConfig(
            @PathVariable String key,
            @Valid @RequestBody UpdateRuntimeConfigRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        RuntimeConfig updated = runtimeConfigService.updateConfig(
                key, request.getValue(), principal.getUsername());
        return ResponseEntity.ok(ApiResponseDto.success("Configuration updated successfully", toDTO(updated)));
    }

    @PostMapping("/{key}/reset")
    public ResponseEntity<ApiResponseDto<RuntimeConfigDTO>> resetConfig(
            @PathVariable String key,
            @AuthenticationPrincipal UserPrincipal principal) {
        RuntimeConfig reset = runtimeConfigService.resetConfig(key, principal.getUsername());
        return ResponseEntity.ok(ApiResponseDto.success("Configuration reset to default", toDTO(reset)));
    }

    private RuntimeConfigDTO toDTO(RuntimeConfig config) {
        return RuntimeConfigDTO.builder()
                .id(config.getId())
                .configKey(config.getConfigKey())
                .configValue(config.getConfigValue())
                .defaultValue(config.getDefaultValue())
                .description(config.getDescription())
                .category(config.getCategory())
                .dataType(config.getDataType())
                .updatedBy(config.getUpdatedBy())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
