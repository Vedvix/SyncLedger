package com.vedvix.syncledger.repository;

import com.vedvix.syncledger.model.RuntimeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RuntimeConfigRepository extends JpaRepository<RuntimeConfig, Long> {

    Optional<RuntimeConfig> findByConfigKey(String configKey);

    List<RuntimeConfig> findByCategory(String category);

    List<RuntimeConfig> findAllByOrderByCategoryAscConfigKeyAsc();
}
