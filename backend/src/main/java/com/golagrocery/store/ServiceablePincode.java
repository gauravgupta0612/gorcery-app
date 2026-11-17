package com.golagrocery.store;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "serviceable_pincodes")
@Getter @Setter
public class ServiceablePincode {
    @Id
    private String pincode;
    private String city;
    private String area;
    private BigDecimal deliveryFee;
    private int etaMinutes = 30;
    private boolean active = true;
}
