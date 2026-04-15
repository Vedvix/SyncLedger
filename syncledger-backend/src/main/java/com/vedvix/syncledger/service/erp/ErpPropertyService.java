package com.vedvix.syncledger.service.erp;

import com.vedvix.syncledger.exception.BadRequestException;
import com.vedvix.syncledger.model.ErpProperty;
import com.vedvix.syncledger.model.ErpType;
import com.vedvix.syncledger.repository.ErpPropertyRepository;
import com.vedvix.syncledger.service.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Manages ERP properties for organizations. Handles encryption/decryption of secrets
 * and validation of required properties per ERP type.
 *
 * @author vedvix
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ErpPropertyService {

    private final ErpPropertyRepository erpPropertyRepository;
    private final EncryptionService encryptionService;

    /**
     * Get all decrypted properties for an org + ERP type as a flat key-value map.
     */
    public Map<String, String> getDecryptedProperties(Long organizationId, ErpType erpType) {
        List<ErpProperty> rows = erpPropertyRepository.findByOrganizationIdAndErpType(organizationId, erpType);
        Map<String, String> props = new LinkedHashMap<>();
        for (ErpProperty row : rows) {
            String value = row.getPropertyValue();
            if (Boolean.TRUE.equals(row.getIsEncrypted()) && value != null && !value.isBlank()) {
                value = decryptSafe(value);
            }
            props.put(row.getPropertyKey(), value);
        }
        return props;
    }

    /**
     * Get all properties with secrets masked for display.
     */
    public Map<String, String> getMaskedProperties(Long organizationId, ErpType erpType) {
        List<ErpProperty> rows = erpPropertyRepository.findByOrganizationIdAndErpType(organizationId, erpType);
        Map<String, String> props = new LinkedHashMap<>();
        for (ErpProperty row : rows) {
            String value = row.getPropertyValue();
            if (Boolean.TRUE.equals(row.getIsEncrypted()) && value != null && !value.isBlank()) {
                // Decrypt then mask
                String decrypted = decryptSafe(value);
                value = EncryptionService.maskSecret(decrypted);
            }
            props.put(row.getPropertyKey(), value);
        }
        return props;
    }

    /**
     * Save/update ERP properties for an organization. Secrets are encrypted.
     * Only non-null values are updated (partial update).
     */
    @Transactional
    public void saveProperties(Long organizationId, ErpType erpType, Map<String, String> properties) {
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (value == null) continue; // skip nulls — don't overwrite existing

            boolean isSecret = ErpPropertyDefinitions.isSecret(erpType, key);
            // Skip blank secrets (don't overwrite existing encrypted value with empty)
            if (isSecret && value.isBlank()) continue;

            String storedValue = isSecret ? encryptionService.encrypt(value) : value;

            Optional<ErpProperty> existing = erpPropertyRepository
                    .findByOrganizationIdAndErpTypeAndPropertyKey(organizationId, erpType, key);

            if (existing.isPresent()) {
                ErpProperty prop = existing.get();
                prop.setPropertyValue(storedValue);
                prop.setIsEncrypted(isSecret);
                erpPropertyRepository.save(prop);
            } else {
                erpPropertyRepository.save(ErpProperty.builder()
                        .organizationId(organizationId)
                        .erpType(erpType)
                        .propertyKey(key)
                        .propertyValue(storedValue)
                        .isEncrypted(isSecret)
                        .build());
            }
        }
    }

    /**
     * Validate that all required properties are present for the given ERP type.
     * Returns list of missing required property keys.
     */
    public List<String> validateRequired(Long organizationId, ErpType erpType) {
        Map<String, String> props = getDecryptedProperties(organizationId, erpType);
        List<String> required = ErpPropertyDefinitions.getRequiredKeys(erpType);
        List<String> missing = new ArrayList<>();
        for (String key : required) {
            String val = props.get(key);
            if (val == null || val.isBlank()) {
                missing.add(key);
            }
        }
        return missing;
    }

    /**
     * Delete all properties for an org + ERP type.
     */
    @Transactional
    public void deleteProperties(Long organizationId, ErpType erpType) {
        erpPropertyRepository.deleteAllByOrganizationIdAndErpType(organizationId, erpType);
    }

    /**
     * Get a single decrypted property value, or null if not found.
     */
    public String getProperty(Long organizationId, ErpType erpType, String key) {
        return erpPropertyRepository
                .findByOrganizationIdAndErpTypeAndPropertyKey(organizationId, erpType, key)
                .map(prop -> {
                    if (Boolean.TRUE.equals(prop.getIsEncrypted()) && prop.getPropertyValue() != null) {
                        return decryptSafe(prop.getPropertyValue());
                    }
                    return prop.getPropertyValue();
                })
                .orElse(null);
    }

    private String decryptSafe(String encrypted) {
        if (encrypted == null || encrypted.isBlank()) return null;
        try {
            return encryptionService.decrypt(encrypted);
        } catch (Exception e) {
            log.error("ERP property decryption failed - encryption key may have changed", e);
            throw new BadRequestException(
                    "ERP configuration is corrupted. Please reconfigure your ERP integration.");
        }
    }
}
