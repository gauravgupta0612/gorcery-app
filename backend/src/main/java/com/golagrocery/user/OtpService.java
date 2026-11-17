package com.golagrocery.user;

import com.golagrocery.common.ApiException;
import com.golagrocery.common.Crypto;
import com.golagrocery.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class OtpService {
    private final OtpRepository repo;
    private final PasswordEncoder encoder;
    private final SmsSender sms;
    private final AppProperties props;

    /** @return the plain OTP only when dev-echo is enabled, otherwise null. */
    @Transactional
    public String issue(String phone, String purpose) {
        if (repo.countByPhoneAndCreatedAtAfter(phone, Instant.now().minus(10, ChronoUnit.MINUTES)) >= 3) {
            throw ApiException.tooMany("Too many OTP requests. Please try again in 10 minutes.");
        }
        String code = Crypto.randomDigits(props.otp().length());
        OtpCode otp = new OtpCode();
        otp.setPhone(phone);
        otp.setPurpose(purpose);
        otp.setCodeHash(encoder.encode(code));
        otp.setExpiresAt(Instant.now().plus(props.otp().ttlMinutes(), ChronoUnit.MINUTES));
        repo.save(otp);
        sms.send(phone, code + " is your Gola Grocery verification code. Valid for " + props.otp().ttlMinutes()
            + " minutes. Do not share it with anyone.");
        return props.otp().devEcho() ? code : null;
    }

    /** Runs in its own transaction so failed attempts are counted even when the caller rolls back. */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW, noRollbackFor = ApiException.class)
    public void verify(String phone, String purpose, String code) {
        OtpCode otp = repo.findFirstByPhoneAndPurposeAndConsumedFalseOrderByCreatedAtDesc(phone, purpose)
            .orElseThrow(() -> ApiException.badRequest("No OTP requested for this number. Please request a new one."));
        if (otp.getExpiresAt().isBefore(Instant.now())) {
            throw ApiException.badRequest("OTP has expired. Please request a new one.");
        }
        if (otp.getAttempts() >= props.otp().maxAttempts()) {
            throw ApiException.tooMany("Too many incorrect attempts. Please request a new OTP.");
        }
        if (!encoder.matches(code, otp.getCodeHash())) {
            otp.setAttempts(otp.getAttempts() + 1);
            repo.save(otp);
            throw ApiException.badRequest("Incorrect OTP");
        }
        otp.setConsumed(true);
        repo.save(otp);
    }
}
