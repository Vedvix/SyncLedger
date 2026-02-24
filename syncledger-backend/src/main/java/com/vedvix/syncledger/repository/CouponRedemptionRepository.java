package com.vedvix.syncledger.repository;

import com.vedvix.syncledger.model.CouponRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for coupon redemption audit trail.
 *
 * @author vedvix
 */
@Repository
public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, Long> {

    List<CouponRedemption> findByCouponIdOrderByRedeemedAtDesc(Long couponId);

    List<CouponRedemption> findByOrganizationIdOrderByRedeemedAtDesc(Long organizationId);

    boolean existsByCouponIdAndOrganizationId(Long couponId, Long organizationId);
}
