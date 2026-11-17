package com.golagrocery.notification;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "notifications")
@Getter @Setter
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String title;
    private String body;
    private String type = "GENERAL";
    private String reference;
    private boolean isRead;
    private Instant createdAt = Instant.now();
}
