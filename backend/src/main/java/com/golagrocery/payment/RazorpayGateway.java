package com.golagrocery.payment;

import com.golagrocery.common.Crypto;
import com.golagrocery.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/** Razorpay Orders + Refunds REST API. Signature checks follow Razorpay's documented HMAC-SHA256 scheme. */
@Slf4j
public class RazorpayGateway implements PaymentGateway {
    private final AppProperties.Razorpay cfg;
    private final RestClient http;

    public RazorpayGateway(AppProperties.Razorpay cfg) {
        if (cfg.keyId() == null || cfg.keyId().isBlank() || cfg.keySecret() == null || cfg.keySecret().isBlank()) {
            throw new IllegalStateException("PAYMENTS_MODE=razorpay requires RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET");
        }
        this.cfg = cfg;
        String basic = Base64.getEncoder().encodeToString((cfg.keyId() + ":" + cfg.keySecret()).getBytes(StandardCharsets.UTF_8));
        this.http = RestClient.builder().baseUrl("https://api.razorpay.com/v1")
            .defaultHeader("Authorization", "Basic " + basic).build();
    }

    @Override public String provider() { return "RAZORPAY"; }
    @Override public String publicKey() { return cfg.keyId(); }
    @Override public boolean testMode() { return cfg.keyId().startsWith("rzp_test_"); }

    @Override
    public String createOrder(String receipt, long amountPaise) {
        try {
            JsonNode res = http.post().uri("/orders").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", amountPaise, "currency", "INR", "receipt", receipt, "payment_capture", 1))
                .retrieve().body(JsonNode.class);
            if (res == null || !res.hasNonNull("id")) throw new IllegalStateException("Razorpay order creation returned no id");
            return res.get("id").asText();
        } catch (RestClientException e) {
            log.error("Razorpay order creation failed: {}", e.getMessage());
            throw new IllegalStateException("Payment gateway unavailable");
        }
    }

    @Override
    public boolean verifyPaymentSignature(String providerOrderId, String providerPaymentId, String signature) {
        String expected = Crypto.hmacSha256Hex(cfg.keySecret(), providerOrderId + "|" + providerPaymentId);
        return Crypto.constantTimeEquals(expected, signature);
    }

    public boolean verifyWebhookSignature(String body, String signature) {
        if (cfg.webhookSecret() == null || cfg.webhookSecret().isBlank()) return false;
        return Crypto.constantTimeEquals(Crypto.hmacSha256Hex(cfg.webhookSecret(), body), signature);
    }

    @Override
    public RefundResult refund(String providerPaymentId, long amountPaise, String note) {
        try {
            JsonNode res = http.post().uri("/payments/{id}/refund", providerPaymentId).contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", amountPaise, "notes", Map.of("reason", note == null ? "" : note)))
                .retrieve().body(JsonNode.class);
            if (res == null || !res.hasNonNull("id")) throw new IllegalStateException("Refund returned no id");
            return new RefundResult(res.get("id").asText(), "processed".equals(res.path("status").asText()));
        } catch (RestClientException e) {
            log.error("Razorpay refund failed: {}", e.getMessage());
            throw new IllegalStateException("Refund could not be initiated with the payment gateway");
        }
    }
}
