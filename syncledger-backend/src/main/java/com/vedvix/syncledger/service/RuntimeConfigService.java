package com.vedvix.syncledger.service;

import com.vedvix.syncledger.model.RuntimeConfig;
import com.vedvix.syncledger.repository.RuntimeConfigRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing runtime configuration with an in-memory cache.
 * 
 * On startup:
 *   1. Loads all configs from the database
 *   2. For any config where the DB value matches the default, checks if an env var override exists
 *   3. Caches all values in a ConcurrentHashMap
 * 
 * On admin update:
 *   1. Updates DB
 *   2. Updates cache
 *   3. Applies side-effects (e.g., logging level changes)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuntimeConfigService {

    private final RuntimeConfigRepository configRepository;
    private final LoggingSystem loggingSystem;
    private final Environment environment;

    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    // Mapping from config keys to Spring property names for env var resolution
    private static final Map<String, String> ENV_PROPERTY_MAP = Map.ofEntries(
        Map.entry("cors.allowed-origins", "cors.allowed-origins"),
        Map.entry("invoice.auto-approval.confidence-threshold", "invoice.auto-approval.confidence-threshold"),
        Map.entry("email.polling.enabled", "email.polling.enabled"),
        Map.entry("email.polling.interval", "email.polling.interval"),
        Map.entry("email.polling.max-emails-per-batch", "email.polling.max-emails-per-batch"),
        Map.entry("pdf-service.url", "pdf-service.url"),
        Map.entry("pdf-service.timeout", "pdf-service.timeout"),
        Map.entry("subscription.scheduler.cron", "subscription.scheduler.cron")
    );

    @PostConstruct
    public void init() {
        loadAllConfigs();
        applyLoggingLevels();
        log.info("RuntimeConfigService initialized with {} cached configs", cache.size());
    }

    /**
     * Load all configs from DB into cache, applying env var overrides where the
     * DB value still equals the default (i.e., never been changed by admin).
     */
    private void loadAllConfigs() {
        List<RuntimeConfig> configs = configRepository.findAll();
        for (RuntimeConfig config : configs) {
            String value = config.getConfigValue();

            // If the value in DB is still the seeded default, check for env var override
            if (value.equals(config.getDefaultValue())) {
                String envProperty = ENV_PROPERTY_MAP.get(config.getConfigKey());
                if (envProperty != null) {
                    String envValue = environment.getProperty(envProperty);
                    if (envValue != null && !envValue.equals(value)) {
                        value = envValue;
                        // Persist the env var override so it shows in the admin UI
                        config.setConfigValue(envValue);
                        config.setUpdatedBy("ENV_OVERRIDE");
                        configRepository.save(config);
                        log.info("Config '{}' overridden from environment: {}", config.getConfigKey(), envValue);
                    }
                }
            }

            cache.put(config.getConfigKey(), value);
        }
    }

    // ── Typed getters ───────────────────────────────────────────

    public String getString(String key, String defaultValue) {
        return cache.getOrDefault(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        String value = cache.get(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid integer for config '{}': {}", key, value);
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = cache.get(key);
        if (value == null) return defaultValue;
        return Boolean.parseBoolean(value.trim());
    }

    public BigDecimal getDecimal(String key, BigDecimal defaultValue) {
        String value = cache.get(key);
        if (value == null) return defaultValue;
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid decimal for config '{}': {}", key, value);
            return defaultValue;
        }
    }

    // ── Admin operations ────────────────────────────────────────

    public List<RuntimeConfig> getAllConfigs() {
        return configRepository.findAllByOrderByCategoryAscConfigKeyAsc();
    }

    public List<RuntimeConfig> getConfigsByCategory(String category) {
        return configRepository.findByCategory(category);
    }

    @Transactional
    public RuntimeConfig updateConfig(String key, String newValue, String updatedBy) {
        RuntimeConfig config = configRepository.findByConfigKey(key)
                .orElseThrow(() -> new IllegalArgumentException("Config not found: " + key));

        validateConfigValue(config, newValue);

        config.setConfigValue(newValue);
        config.setUpdatedBy(updatedBy);
        RuntimeConfig saved = configRepository.save(config);

        // Update cache
        cache.put(key, newValue);

        // Apply side-effects
        applySideEffects(key, newValue);

        log.info("Config '{}' updated to '{}' by {}", key, newValue, updatedBy);
        return saved;
    }

    @Transactional
    public RuntimeConfig resetConfig(String key, String updatedBy) {
        RuntimeConfig config = configRepository.findByConfigKey(key)
                .orElseThrow(() -> new IllegalArgumentException("Config not found: " + key));

        String defaultValue = config.getDefaultValue();
        config.setConfigValue(defaultValue);
        config.setUpdatedBy(updatedBy + " (reset)");
        RuntimeConfig saved = configRepository.save(config);

        cache.put(key, defaultValue);
        applySideEffects(key, defaultValue);

        log.info("Config '{}' reset to default '{}' by {}", key, defaultValue, updatedBy);
        return saved;
    }

    // ── Validation ──────────────────────────────────────────────

    private void validateConfigValue(RuntimeConfig config, String newValue) {
        switch (config.getDataType()) {
            case "INTEGER" -> {
                try {
                    Integer.parseInt(newValue.trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Value must be a valid integer for key: " + config.getConfigKey());
                }
            }
            case "DECIMAL" -> {
                try {
                    BigDecimal val = new BigDecimal(newValue.trim());
                    if (config.getConfigKey().contains("confidence-threshold")) {
                        if (val.compareTo(BigDecimal.ZERO) < 0 || val.compareTo(BigDecimal.ONE) > 0) {
                            throw new IllegalArgumentException("Confidence threshold must be between 0.0 and 1.0");
                        }
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Value must be a valid decimal for key: " + config.getConfigKey());
                }
            }
            case "BOOLEAN" -> {
                if (!"true".equalsIgnoreCase(newValue.trim()) && !"false".equalsIgnoreCase(newValue.trim())) {
                    throw new IllegalArgumentException("Value must be 'true' or 'false' for key: " + config.getConfigKey());
                }
            }
        }
    }

    // ── Side-effects ────────────────────────────────────────────

    private void applySideEffects(String key, String value) {
        if (key.startsWith("logging.level.")) {
            applyLoggingLevel(key, value);
        }
    }

    private void applyLoggingLevels() {
        cache.forEach((key, value) -> {
            if (key.startsWith("logging.level.")) {
                applyLoggingLevel(key, value);
            }
        });
    }

    private void applyLoggingLevel(String key, String value) {
        String loggerName = key.substring("logging.level.".length());
        try {
            LogLevel level = LogLevel.valueOf(value.toUpperCase().trim());
            loggingSystem.setLogLevel(loggerName, level);
            log.info("Applied logging level {} for logger '{}'", level, loggerName);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid logging level '{}' for logger '{}', ignoring", value, loggerName);
        }
    }
}
