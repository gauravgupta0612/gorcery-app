package com.golagrocery.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByProviderOrderId(String providerOrderId);
    Optional<Payment> findFirstByOrderIdOrderByIdDesc(Long orderId);
    List<Payment> findByOrderIdOrderByIdDesc(Long orderId);
    Optional<Payment> findFirstByOrderIdAndStatusOrderByIdDesc(Long orderId, String status);
}
