package com.golagrocery.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Optional<Notification> findByIdAndUserId(Long id, Long userId);
    long countByUserIdAndIsReadFalse(Long userId);

    @Modifying
    @Query("update Notification n set n.isRead = true where n.userId = :userId and n.isRead = false")
    int markAllRead(Long userId);

    @Modifying
    @Query(value = """
        insert into notifications(user_id, title, body, type, reference)
        select distinct ur.user_id, :title, :body, :type, :reference from user_roles ur
        join roles r on r.id = ur.role_id join users u on u.id = ur.user_id where r.name = 'CUSTOMER' and u.enabled
        """, nativeQuery = true)
    int broadcastToCustomers(String title, String body, String type, String reference);
}
