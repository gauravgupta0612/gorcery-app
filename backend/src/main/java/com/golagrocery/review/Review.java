package com.golagrocery.review;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "reviews")
@Getter @Setter
public class Review {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long productId;
    private Long userId;
    private Long orderId;
    private Short rating;
    private String title;
    private String comment;
    private boolean approved = true;
    private Instant createdAt = Instant.now();
}
