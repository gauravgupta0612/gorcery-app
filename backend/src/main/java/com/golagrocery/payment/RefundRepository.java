package com.golagrocery.payment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, Long> {
    List<Refund> findByOrderIdOrderByIdDesc(Long orderId);
    Page<Refund> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Refund> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    Optional<Refund> findByProviderRefundId(String providerRefundId);

    @Query("select coalesce(sum(r.amount), 0) from Refund r where r.orderId = :orderId and r.status in ('REQUESTED','APPROVED','PROCESSED')")
    BigDecimal sumActiveForOrder(Long orderId);
}
