package com.golagrocery.delivery;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, Long> {
    Optional<DeliveryAssignment> findFirstByOrderIdAndStatusNot(Long orderId, String status);
    List<DeliveryAssignment> findByDeliveryPartnerIdAndStatusInOrderByAssignedAtAsc(Long partnerId, Collection<String> statuses);
    Page<DeliveryAssignment> findByDeliveryPartnerIdAndStatusOrderByDeliveredAtDesc(Long partnerId, String status, Pageable pageable);
    long countByDeliveryPartnerIdAndStatusIn(Long partnerId, Collection<String> statuses);
    long countByDeliveryPartnerIdAndStatus(Long partnerId, String status);
}
