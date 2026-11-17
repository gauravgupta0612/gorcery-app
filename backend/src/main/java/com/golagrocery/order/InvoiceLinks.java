package com.golagrocery.order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Unguessable, login-free download links for invoice PDFs (needed by WhatsApp, which fetches the document itself).
 * The token is an HMAC of the order id with the JWT secret, so links cannot be forged or enumerated.
 */
@Component
public class InvoiceLinks {
    private final byte[] key;
    private final String baseUrl;

    public InvoiceLinks(@Value("${app.jwt.secret:}") String secret,
                        @Value("${app.public-base-url:https://gola-grocery-api.onrender.com}") String baseUrl) {
        this.key = ("invoice-link:" + secret).getBytes(StandardCharsets.UTF_8);
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public String token(long orderId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] sig = mac.doFinal(("order:" + orderId).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(sig).substring(0, 32);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean valid(long orderId, String token) {
        return token != null && MessageDigest.isEqual(token(orderId).getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));
    }

    public String url(long orderId, String orderNumber) {
        return baseUrl + "/api/v1/public/invoices/" + orderId + "/" + token(orderId) + "/Invoice-" + orderNumber + ".pdf";
    }
}
