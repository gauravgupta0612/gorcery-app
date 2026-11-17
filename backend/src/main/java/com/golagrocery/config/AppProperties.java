package com.golagrocery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Otp otp, Cors cors, Storage storage, Payments payments,
                            Firebase firebase, BootstrapAdmin bootstrapAdmin) {

    public record Jwt(String secret, int accessTokenMinutes, int refreshTokenDays) {}
    public record Otp(int length, int ttlMinutes, int maxAttempts, boolean devEcho) {}
    public record Cors(List<String> allowedOrigins) {}
    public record Storage(String dir, String seedDir) {}
    public record Payments(String mode, Razorpay razorpay) {
        public boolean isRazorpay() { return "razorpay".equalsIgnoreCase(mode); }
    }
    public record Razorpay(String keyId, String keySecret, String webhookSecret) {}
    public record Firebase(String credentialsFile) {}
    public record BootstrapAdmin(String phone, String email, String password, String name) {}
}
