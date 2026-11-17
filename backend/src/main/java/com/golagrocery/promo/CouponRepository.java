package com.golagrocery.promo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCodeIgnoreCase(String code);
    List<Coupon> findByActiveTrueOrderByMinOrderAmountAsc();
    List<Coupon> findAllByOrderByCreatedAtDesc();

    @Modifying
    @Query(value = "update coupons set used_count = used_count + :delta where id = :id", nativeQuery = true)
    void addUsage(Long id, int delta);
}
