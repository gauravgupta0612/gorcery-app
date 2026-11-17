package com.golagrocery.catalog;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "product_variants")
@Getter @Setter
public class ProductVariant {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    private String sku;
    private String barcode;
    private String label;
    private BigDecimal quantityValue;
    @Enumerated(EnumType.STRING)
    private Unit unit;
    private BigDecimal mrp;
    private BigDecimal sellingPrice;
    private int maxPerOrder = 10;
    private boolean isDefault;
    private boolean active = true;

    @OneToOne(fetch = FetchType.LAZY)
    @PrimaryKeyJoinColumn
    private Inventory inventory;

    public int stock() { return inventory == null ? 0 : inventory.getQuantity(); }

    public enum Unit { KG, G, L, ML, PCS }
}
