package com.golagrocery.catalog;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "inventory_movements")
@Getter @Setter
public class InventoryMovement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long variantId;
    private int delta;
    private String reason;
    private String reference;
    private Long createdBy;
    private Instant createdAt = Instant.now();
}
