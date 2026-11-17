package com.golagrocery.promo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "coupons")
@Getter @Setter
public class Coupon {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String code;
    private String description;
    private String discountType;   // PERCENT | FLAT
    private BigDecimal discountValue;
    private BigDecimal maxDiscount;
    private BigDecimal minOrderAmount = BigDecimal.ZERO;
    private Integer usageLimit;
    private int perUserLimit = 1;
    private int usedCount;
    private Instant validFrom = Instant.now();
    private Instant validTo;
    private boolean firstOrderOnly;
    private boolean active = true;
    private Instant createdAt = Instant.now();
}
