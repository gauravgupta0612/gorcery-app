package com.golagrocery.user;

import com.golagrocery.user.AuthDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;

    @PostMapping("/register")
    public OtpSentResponse register(@Valid @RequestBody RegisterRequest req) { return auth.register(req); }

    @PostMapping("/otp/request")
    public OtpSentResponse requestOtp(@Valid @RequestBody OtpRequest req) { return auth.requestOtp(req); }

    @PostMapping("/otp/verify")
    public TokenResponse verify(@Valid @RequestBody VerifyOtpRequest req) { return auth.verifyOtp(req); }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) { return auth.login(req); }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest req) { return auth.refresh(req.refreshToken()); }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest req) {
        auth.logout(req.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
