package com.golagrocery.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpCode, Long> {
    Optional<OtpCode> findFirstByPhoneAndPurposeAndConsumedFalseOrderByCreatedAtDesc(String phone, String purpose);
    long countByPhoneAndCreatedAtAfter(String phone, Instant since);
}
