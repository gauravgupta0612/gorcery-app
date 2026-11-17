package com.golagrocery.security;

import java.util.Set;

/** Authenticated principal extracted from a verified JWT. */
public record AuthUser(Long id, String phone, Set<String> roles) {
    public boolean hasRole(String role) { return roles.contains(role); }
}
