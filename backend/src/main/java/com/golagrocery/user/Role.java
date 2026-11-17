package com.golagrocery.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "roles")
@Getter @Setter
public class Role {
    public static final String CUSTOMER = "CUSTOMER";
    public static final String ADMIN = "ADMIN";
    public static final String DELIVERY_PARTNER = "DELIVERY_PARTNER";
    public static final String STORE_MANAGER = "STORE_MANAGER";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short id;
    private String name;
}
