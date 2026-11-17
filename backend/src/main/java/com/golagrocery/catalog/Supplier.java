package com.golagrocery.catalog;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "suppliers")
@Getter @Setter
public class Supplier {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String contactName;
    private String phone;
    private String email;
    private String gstin;
    private String address;
    private boolean active = true;
    private Instant createdAt = Instant.now();
}
