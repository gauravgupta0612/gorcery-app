package com.golagrocery.payment;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "refunds")
@Getter @Setter
public class Refund {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long orderId;
    private Long paymentId;
    private BigDecimal amount;
    private String reason;
    private String status;   // REQUESTED, APPROVED, PROCESSED, REJECTED, FAILED
    private String providerRefundId;
    private Long requestedBy;
    private Long processedBy;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void touch() { updatedAt = Instant.now(); }
}
