package com.golagrocery.order;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter @Setter
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    private Long productId;
    private Long variantId;
    private String productName;
    private String variantLabel;
    private String imageUrl;
    private String sku;
    private BigDecimal unitMrp;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal gstRate;
    private BigDecimal taxAmount;
    private BigDecimal lineTotal;
}
