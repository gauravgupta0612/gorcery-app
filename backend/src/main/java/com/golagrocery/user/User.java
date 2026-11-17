package com.golagrocery.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
@Getter @Setter
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fullName;
    private String phone;
    private String email;
    private String passwordHash;
    private boolean phoneVerified;
    private boolean enabled = true;
    private String preferredLanguage = "en";
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    public Set<String> roleNames() {
        return roles.stream().map(Role::getName).collect(Collectors.toSet());
    }

    @PreUpdate
    void touch() { updatedAt = Instant.now(); }
}
