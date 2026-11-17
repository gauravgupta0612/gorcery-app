package com.golagrocery.support;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByUserIdOrderByCreatedAtDesc(Long userId);
    Page<SupportTicket> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByStatus(String status);
}
