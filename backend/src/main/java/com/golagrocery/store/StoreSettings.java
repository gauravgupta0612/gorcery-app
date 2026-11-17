package com.golagrocery.store;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;

@Entity
@Table(name = "store_settings")
@Getter @Setter
public class StoreSettings {
    @Id
    private Short id = 1;
    private String storeName;
    private String tagline;
    private String supportPhone;
    private String supportEmail;
    private String gstin;
    private String storeAddress;
    private BigDecimal deliveryFee;
    private BigDecimal freeDeliveryAbove;
    private BigDecimal handlingFee;
    private BigDecimal minOrderAmount;
    private boolean codEnabled;
    private BigDecimal codMaxAmount;
    private boolean storeOpen;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private Instant updatedAt = Instant.now();
}
