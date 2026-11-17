package com.golagrocery.notification;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "device_tokens")
@Getter @Setter
public class DeviceToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String token;
    private String platform = "ANDROID";
    private Instant updatedAt = Instant.now();
}
