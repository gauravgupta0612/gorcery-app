package com.golagrocery.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByUserIdOrderByPlacedAtDesc(Long userId, Pageable pageable);
    Optional<Order> findByIdAndUserId(Long id, Long userId);
    Optional<Order> findByOrderNumber(String orderNumber);
    long countByStatus(OrderStatus status);
    List<Order> findByStatusAndPlacedAtBefore(OrderStatus status, Instant before);

    @Query("select count(o) from CustomerOrder o where o.userId = :userId and o.status not in (com.golagrocery.order.OrderStatus.CANCELLED, com.golagrocery.order.OrderStatus.PAYMENT_FAILED, com.golagrocery.order.OrderStatus.PENDING_PAYMENT)")
    long countSuccessfulByUser(@Param("userId") Long userId);

    @Query("""
        select o from CustomerOrder o where o.status in :statuses
          and (:q = '' or o.orderNumber like concat('%', :q, '%') or o.shipPhone like concat('%', :q, '%')
               or lower(o.shipName) like lower(concat('%', :q, '%')))
          and o.placedAt >= :from and o.placedAt < :to
        order by o.placedAt desc
        """)
    /** Pass all statuses for "any", q = "" for no text filter, and a wide date range for no date filter. */
    Page<Order> adminSearch(@Param("statuses") Collection<OrderStatus> statuses, @Param("q") String q,
                            @Param("from") Instant from, @Param("to") Instant to, Pageable pageable);

    @Query(value = "select nextval('order_number_seq')", nativeQuery = true)
    long nextOrderNumber();
}
