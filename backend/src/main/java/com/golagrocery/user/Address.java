package com.golagrocery.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "addresses")
@Getter @Setter
public class Address {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String label;
    private String contactName;
    private String contactPhone;
    private String line1;
    private String line2;
    private String landmark;
    private String city;
    private String state;
    private String pincode;
    private Double latitude;
    private Double longitude;
    private boolean isDefault;
    private Instant createdAt = Instant.now();
}
