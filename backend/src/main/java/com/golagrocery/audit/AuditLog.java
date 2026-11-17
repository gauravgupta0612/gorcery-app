package com.golagrocery.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "audit_logs")
@Getter @Setter
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long actorId;
    private String actorRole;
    private String action;
    private String entityType;
    private String entityId;
    private String details;
    private String ipAddress;
    private Instant createdAt = Instant.now();
}
