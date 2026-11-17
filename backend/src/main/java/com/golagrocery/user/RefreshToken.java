package com.golagrocery.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
@Getter @Setter
public class RefreshToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String tokenHash;
    private Instant expiresAt;
    private boolean revoked;
    private Instant createdAt = Instant.now();
}
