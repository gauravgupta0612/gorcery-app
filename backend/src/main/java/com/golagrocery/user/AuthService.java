package com.golagrocery.user;

import com.golagrocery.audit.AuditService;
import com.golagrocery.common.ApiException;
import com.golagrocery.common.Crypto;
import com.golagrocery.config.AppProperties;
import com.golagrocery.security.JwtService;
import com.golagrocery.user.AuthDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final OtpService otpService;
    private final JwtService jwt;
    private final AppProperties props;
    private final AuditService audit;

    @Transactional
    public OtpSentResponse register(RegisterRequest req) {
        String email = StringUtils.hasText(req.email()) ? req.email().trim().toLowerCase() : null;
        User user = users.findByPhone(req.phone()).orElse(null);
        if (user != null && user.isPhoneVerified()) {
            throw ApiException.conflict("An account with this mobile number already exists. Please log in.");
        }
        if (email != null) {
            users.findByEmailIgnoreCase(email).ifPresent(other -> {
                if (!other.getPhone().equals(req.phone())) throw ApiException.conflict("This email is already registered");
            });
        }
        if (user == null) {
            user = new User();
            user.setPhone(req.phone());
            user.getRoles().add(roles.findByName(Role.CUSTOMER).orElseThrow());
        }
        user.setFullName(req.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(req.password()));
        users.save(user);
        String dev = otpService.issue(req.phone(), "REGISTER");
        return new OtpSentResponse(true, req.phone(), dev);
    }

    @Transactional
    public OtpSentResponse requestOtp(OtpRequest req) {
        User user = users.findByPhone(req.phone()).orElse(null);
        if ("LOGIN".equals(req.purpose())) {
            if (user == null || !user.isPhoneVerified()) {
                throw ApiException.notFound("Account");
            }
            if (!user.isEnabled()) throw ApiException.forbidden("This account has been disabled. Please contact support.");
        } else if (user == null) {
            throw ApiException.badRequest("Please register first");
        }
        return new OtpSentResponse(true, req.phone(), otpService.issue(req.phone(), req.purpose()));
    }

    @Transactional
    public TokenResponse verifyOtp(VerifyOtpRequest req) {
        User user = users.findByPhone(req.phone()).orElseThrow(() -> ApiException.notFound("Account"));
        otpService.verify(req.phone(), req.purpose(), req.code());
        if (!user.isEnabled()) throw ApiException.forbidden("This account has been disabled. Please contact support.");
        if (!user.isPhoneVerified()) {
            user.setPhoneVerified(true);
            users.save(user);
            audit.logAs(user.getId(), "CUSTOMER", "USER_REGISTERED", "User", user.getId(), null);
        }
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest req) {
        String id = req.identifier().trim();
        User user = (id.contains("@") ? users.findByEmailIgnoreCase(id) : users.findByPhone(id))
            .orElseThrow(() -> ApiException.unauthorized("Invalid credentials"));
        if (user.getPasswordHash() == null || !encoder.matches(req.password(), user.getPasswordHash())) {
            audit.logAs(user.getId(), null, "LOGIN_FAILED", "User", user.getId(), null);
            throw ApiException.unauthorized("Invalid credentials");
        }
        if (!user.isPhoneVerified()) throw ApiException.forbidden("Please verify your mobile number with OTP first");
        if (!user.isEnabled()) throw ApiException.forbidden("This account has been disabled. Please contact support.");
        audit.logAs(user.getId(), String.join(",", user.roleNames()), "LOGIN", "User", user.getId(), null);
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse refresh(String rawToken) {
        RefreshToken t = refreshTokens.findByTokenHash(Crypto.sha256Hex(rawToken))
            .orElseThrow(() -> ApiException.unauthorized("Session expired. Please log in again."));
        if (t.isRevoked()) {
            // Token reuse detected - revoke the whole family for safety.
            refreshTokens.revokeAllForUser(t.getUserId());
            throw ApiException.unauthorized("Session expired. Please log in again.");
        }
        if (t.getExpiresAt().isBefore(Instant.now())) throw ApiException.unauthorized("Session expired. Please log in again.");
        t.setRevoked(true);
        refreshTokens.save(t);
        User user = users.findById(t.getUserId()).orElseThrow(() -> ApiException.unauthorized("Account not found"));
        if (!user.isEnabled()) throw ApiException.forbidden("This account has been disabled");
        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawToken) {
        refreshTokens.findByTokenHash(Crypto.sha256Hex(rawToken)).ifPresent(t -> {
            t.setRevoked(true);
            refreshTokens.save(t);
        });
    }

    private TokenResponse issueTokens(User user) {
        String raw = Crypto.randomToken(32);
        RefreshToken rt = new RefreshToken();
        rt.setUserId(user.getId());
        rt.setTokenHash(Crypto.sha256Hex(raw));
        rt.setExpiresAt(Instant.now().plus(props.jwt().refreshTokenDays(), ChronoUnit.DAYS));
        refreshTokens.save(rt);
        String access = jwt.createAccessToken(user.getId(), user.getPhone(), user.roleNames());
        return new TokenResponse(access, raw, jwt.accessTokenTtlSeconds(), UserDto.of(user));
    }
}
