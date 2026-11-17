package com.golagrocery.payment;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
@Getter @Setter
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long orderId;
    private String provider;       // RAZORPAY | MOCK | COD
    private String method;
    private BigDecimal amount;
    private String currency = "INR";
    private String status;         // CREATED, PAID, FAILED, REFUNDED, COD_PENDING, COD_COLLECTED
    private String providerOrderId;
    private String providerPaymentId;
    private String providerSignature;
    private String failureReason;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void touch() { updatedAt = Instant.now(); }
}
