package com.golagrocery.delivery;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "delivery_partners")
@Getter @Setter
public class DeliveryPartner {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String vehicleType = "BIKE";
    private String vehicleNumber;
    private String licenseNumber;
    private boolean available = true;
    private boolean active = true;
    private Double currentLat;
    private Double currentLng;
    private Instant createdAt = Instant.now();
}
