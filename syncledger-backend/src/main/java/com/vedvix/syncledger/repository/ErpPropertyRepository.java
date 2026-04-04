package com.vedvix.syncledger.repository;

import com.vedvix.syncledger.model.ErpProperty;
import com.vedvix.syncledger.model.ErpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ErpPropertyRepository extends JpaRepository<ErpProperty, Long> {

    List<ErpProperty> findByOrganizationIdAndErpType(Long organizationId, ErpType erpType);

    Optional<ErpProperty> findByOrganizationIdAndErpTypeAndPropertyKey(
            Long organizationId, ErpType erpType, String propertyKey);

    @Modifying
    @Query("DELETE FROM ErpProperty p WHERE p.organizationId = :orgId AND p.erpType = :erpType")
    void deleteAllByOrganizationIdAndErpType(@Param("orgId") Long orgId, @Param("erpType") ErpType erpType);

    boolean existsByOrganizationIdAndErpType(Long organizationId, ErpType erpType);
}
