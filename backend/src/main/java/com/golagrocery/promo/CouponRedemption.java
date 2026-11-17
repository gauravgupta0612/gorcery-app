package com.golagrocery.promo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "coupon_redemptions")
@Getter @Setter
public class CouponRedemption {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long couponId;
    private Long userId;
    private Long orderId;
    private BigDecimal amount;
    private Instant createdAt = Instant.now();
}
