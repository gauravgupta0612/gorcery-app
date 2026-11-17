package com.golagrocery.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "otp_codes")
@Getter @Setter
public class OtpCode {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String phone;
    private String codeHash;
    private String purpose;
    private int attempts;
    private Instant expiresAt;
    private boolean consumed;
    private Instant createdAt = Instant.now();
}
