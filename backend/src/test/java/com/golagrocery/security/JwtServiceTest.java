package com.golagrocery.security;

import com.golagrocery.config.AppProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private static AppProperties props(String secret) {
        return new AppProperties(new AppProperties.Jwt(secret, 15, 30), null, null, null, null, null, null);
    }

    @Test
    void roundTripAndTamperDetection() {
        JwtService jwt = new JwtService(props(java.util.Base64.getEncoder().encodeToString("an-example-test-secret-that-is-long-enough-123456".getBytes())));
        String token = jwt.createAccessToken(42L, "9876543210", List.of("CUSTOMER"));
        AuthUser u = jwt.parse(token).orElseThrow();
        assertEquals(42L, u.id());
        assertTrue(u.hasRole("CUSTOMER"));
        assertTrue(jwt.parse(token + "x").isEmpty());

        JwtService other = new JwtService(props(java.util.Base64.getEncoder().encodeToString("a-different-secret-that-is-also-long-enough-987654".getBytes())));
        assertTrue(other.parse(token).isEmpty(), "token signed with another key must be rejected");
    }

    @Test
    void refusesMissingOrShortSecret() {
        assertThrows(IllegalStateException.class, () -> new JwtService(props("")));
        assertThrows(IllegalStateException.class, () -> new JwtService(props("c2hvcnQ=")));
    }
}
