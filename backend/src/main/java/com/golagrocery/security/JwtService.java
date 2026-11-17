package com.golagrocery.security;

import com.golagrocery.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class JwtService {
    private static final String ISSUER = "gola-grocery";
    private final SecretKey key;
    private final int accessMinutes;

    public JwtService(AppProperties props) {
        String secret = props.jwt().secret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET environment variable is required (>= 32 bytes). Generate with: openssl rand -base64 48");
        }
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(secret);
        } catch (RuntimeException e) {
            bytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.accessMinutes = props.jwt().accessTokenMinutes();
    }

    public String createAccessToken(Long userId, String phone, Collection<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
            .issuer(ISSUER)
            .subject(String.valueOf(userId))
            .claim("phone", phone)
            .claim("roles", new ArrayList<>(roles))
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(accessMinutes, ChronoUnit.MINUTES)))
            .id(UUID.randomUUID().toString())
            .signWith(key)
            .compact();
    }

    public long accessTokenTtlSeconds() { return accessMinutes * 60L; }

    /** @return the principal, or empty when the token is invalid/expired. */
    public Optional<AuthUser> parse(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key).requireIssuer(ISSUER).build()
                .parseSignedClaims(token).getPayload();
            @SuppressWarnings("unchecked")
            List<String> roles = c.get("roles", List.class);
            return Optional.of(new AuthUser(Long.valueOf(c.getSubject()), c.get("phone", String.class),
                roles == null ? Set.of() : Set.copyOf(roles)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
