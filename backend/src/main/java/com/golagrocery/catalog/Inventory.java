package com.golagrocery.catalog;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "inventory")
@Getter @Setter
public class Inventory {
    @Id
    private Long variantId;
    private int quantity;
    private int reserved;
    private int lowStockThreshold = 10;
    @Version
    private long version;
    private Instant updatedAt = Instant.now();

    public String status() {
        if (quantity <= 0) return "OUT_OF_STOCK";
        if (quantity <= lowStockThreshold) return "LOW_STOCK";
        return "IN_STOCK";
    }
}
