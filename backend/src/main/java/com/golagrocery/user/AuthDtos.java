package com.golagrocery.user;

import jakarta.validation.constraints.*;

import java.util.Set;

public final class AuthDtos {
    private AuthDtos() {}
    public static final String PHONE_REGEX = "^[6-9]\\d{9}$";

    public record RegisterRequest(
        @NotBlank @Size(min = 2, max = 120) String fullName,
        @NotBlank @Pattern(regexp = PHONE_REGEX, message = "Enter a valid 10-digit Indian mobile number") String phone,
        @Email @Size(max = 160) String email,
        @NotBlank @Size(min = 8, max = 72, message = "Password must be 8-72 characters") String password) {}

    public record OtpRequest(
        @NotBlank @Pattern(regexp = PHONE_REGEX, message = "Enter a valid 10-digit Indian mobile number") String phone,
        @NotBlank @Pattern(regexp = "REGISTER|LOGIN") String purpose) {}

    public record VerifyOtpRequest(
        @NotBlank @Pattern(regexp = PHONE_REGEX) String phone,
        @NotBlank @Pattern(regexp = "\\d{4,8}", message = "Enter the OTP") String code,
        @NotBlank @Pattern(regexp = "REGISTER|LOGIN") String purpose) {}

    public record LoginRequest(@NotBlank @Size(max = 160) String identifier, @NotBlank @Size(max = 72) String password) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record OtpSentResponse(boolean otpSent, String phone, String devOtp) {}

    public record UserDto(Long id, String fullName, String phone, String email, Set<String> roles, String preferredLanguage) {
        public static UserDto of(User u) {
            return new UserDto(u.getId(), u.getFullName(), u.getPhone(), u.getEmail(), u.roleNames(), u.getPreferredLanguage());
        }
    }

    public record TokenResponse(String accessToken, String refreshToken, long expiresIn, UserDto user) {}
}
