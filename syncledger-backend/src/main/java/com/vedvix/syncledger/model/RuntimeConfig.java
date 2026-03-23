package com.vedvix.syncledger.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "runtime_configs", indexes = {
    @Index(name = "idx_runtime_config_key", columnList = "config_key"),
    @Index(name = "idx_runtime_config_category", columnList = "category")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuntimeConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_key", nullable = false, unique = true, length = 255)
    private String configKey;

    @Column(name = "config_value", nullable = false, columnDefinition = "TEXT")
    private String configValue;

    @Column(name = "default_value", nullable = false, columnDefinition = "TEXT")
    private String defaultValue;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(name = "data_type", nullable = false, length = 50)
    private String dataType;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
