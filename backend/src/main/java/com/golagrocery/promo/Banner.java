package com.golagrocery.promo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "banners")
@Getter @Setter
public class Banner {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String subtitle;
    private String imageUrl;
    private String linkType = "NONE";
    private String linkValue;
    private String bgColor;
    private int sortOrder;
    private boolean active = true;
    private Instant startsAt;
    private Instant endsAt;
}
