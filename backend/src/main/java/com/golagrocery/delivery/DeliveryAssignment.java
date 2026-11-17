package com.golagrocery.delivery;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "delivery_assignments")
@Getter @Setter
public class DeliveryAssignment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long orderId;
    private Long deliveryPartnerId;
    private String status;    // ASSIGNED, PICKED_UP, OUT_FOR_DELIVERY, DELIVERED, CANCELLED
    private Instant assignedAt = Instant.now();
    private Instant pickedUpAt;
    private Instant deliveredAt;
    private BigDecimal codCollected;
}
