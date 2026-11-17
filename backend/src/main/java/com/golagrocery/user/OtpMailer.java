package com.golagrocery.user;

import com.golagrocery.common.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Sends OTP codes by email. Two free transports:
 *  1. HTTPS relay (MAIL_RELAY_URL + MAIL_RELAY_SECRET) - a Google Apps Script that sends from your Gmail.
 *     Works on hosts that block SMTP ports (e.g. Render free).
 *  2. SMTP (SPRING_MAIL_* + MAIL_SMTP_ENABLED=true) - e.g. Gmail/Brevo on hosts that allow port 587.
 * When neither is configured, OTP delivery falls back to the SMS sender / dev echo.
 */
@Slf4j
@Component
public class OtpMailer {
    private final ObjectProvider<JavaMailSender> mailSender;
    private final String smtpHost;
    private final boolean smtpEnabled;
    private final String from;
    private final String relayUrl;
    private final String relaySecret;
    private final ObjectMapper json = new ObjectMapper();
    /** Apps Script answers a POST with a 302 to googleusercontent.com; the result must be fetched with GET. */
    private final HttpClient http = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(10)).build();

    public OtpMailer(ObjectProvider<JavaMailSender> mailSender,
                     @Value("${spring.mail.host:}") String smtpHost,
                     @Value("${app.mail.smtp-enabled:false}") boolean smtpEnabled,
                     @Value("${app.mail.from:}") String from,
                     @Value("${app.mail.relay-url:}") String relayUrl,
                     @Value("${app.mail.relay-secret:}") String relaySecret) {
        this.mailSender = mailSender;
        this.smtpHost = smtpHost;
        this.smtpEnabled = smtpEnabled;
        this.from = from;
        this.relayUrl = relayUrl;
        this.relaySecret = relaySecret;
    }

    private boolean relay() { return StringUtils.hasText(relayUrl) && StringUtils.hasText(relaySecret); }
    private boolean smtp() { return smtpEnabled && StringUtils.hasText(smtpHost) && StringUtils.hasText(from) && mailSender.getIfAvailable() != null; }

    public boolean enabled() { return relay() || smtp(); }

    public void send(String to, String code, int ttlMinutes) {
        String subject = code + " is your Gola Grocery OTP";
        try {
            deliver(to, subject, body(code, ttlMinutes), null, null);
        } catch (Exception e) {
            log.error("OTP email to {} failed: {}", mask(to), e.getMessage());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "EMAIL_FAILED", "Couldn't send the OTP email. Please try again in a minute.");
        }
    }

    /**
     * Sends any transactional email (e.g. the order invoice) with an optional PDF attachment.
     * Throws on failure; callers decide whether that matters.
     */
    public void sendDocument(String to, String subject, String html, String attachmentName, byte[] pdf) throws Exception {
        if (!enabled()) throw new IllegalStateException("email is not configured");
        deliver(to, subject, html, attachmentName, pdf);
    }

    private void deliver(String to, String subject, String html, String attachmentName, byte[] pdf) throws Exception {
        if (relay()) {
            Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("secret", relaySecret);
            body.put("to", to);
            body.put("subject", subject);
            body.put("html", html);
            if (pdf != null) {
                body.put("attachments", java.util.List.of(Map.of(
                    "name", attachmentName, "mimeType", "application/pdf",
                    "base64", java.util.Base64.getEncoder().encodeToString(pdf))));
            }
            HttpRequest req = HttpRequest.newBuilder(URI.create(relayUrl)).timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            Map<?, ?> res;
            try {
                res = json.readValue(resp.body(), Map.class);
            } catch (Exception parse) {
                throw new IllegalStateException("relay HTTP " + resp.statusCode() + " (not JSON - is the web app deployed for 'Anyone'?)");
            }
            if (!Boolean.TRUE.equals(res.get("ok"))) {
                throw new IllegalStateException("relay refused: " + res.get("error"));
            }
            return;
        }
        JavaMailSender sender = mailSender.getObject();
        var msg = sender.createMimeMessage();
        var h = new MimeMessageHelper(msg, pdf != null, "UTF-8");
        h.setFrom(from, "Gola Grocery");
        h.setTo(to);
        h.setSubject(subject);
        h.setText(html, true);
        if (pdf != null) h.addAttachment(attachmentName, new org.springframework.core.io.ByteArrayResource(pdf), "application/pdf");
        sender.send(msg);
    }

    private static String body(String code, int ttl) {
        return """
            <div style="font-family:Arial,sans-serif;max-width:420px;margin:auto;border:1px solid #e6e8eb;border-radius:12px;overflow:hidden">
              <div style="background:#0C831F;color:#fff;padding:16px 20px;font-size:18px;font-weight:bold">Gola Grocery</div>
              <div style="padding:20px">
                <p>Your verification code is / आपका सत्यापन कोड है:</p>
                <p style="font-size:32px;font-weight:bold;letter-spacing:8px;color:#0B3D16;margin:8px 0">%s</p>
                <p style="color:#6b7280">Valid for %d minutes. Never share this code with anyone.<br>यह कोड %d मिनट तक मान्य है। इसे किसी के साथ साझा न करें।</p>
                <p style="color:#6b7280;font-size:12px">If you didn't request this, you can ignore this email.</p>
              </div>
              <div style="background:#FFF8DB;color:#0B3D16;padding:10px 20px;font-size:12px">Freshness Delivered to Your Doorstep.</div>
            </div>
            """.formatted(code, ttl, ttl);
    }

    public static String mask(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] p = email.split("@", 2);
        String name = p[0].length() <= 2 ? p[0].charAt(0) + "*" : p[0].charAt(0) + "*".repeat(Math.min(5, p[0].length() - 2)) + p[0].charAt(p[0].length() - 1);
        return name + "@" + p[1];
    }
}
