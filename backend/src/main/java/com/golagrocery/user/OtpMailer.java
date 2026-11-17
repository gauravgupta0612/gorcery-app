package com.golagrocery.user;

import com.golagrocery.common.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Sends OTP codes by email (free with an SMTP provider such as Brevo). Disabled when SPRING_MAIL_HOST is not set. */
@Slf4j
@Component
public class OtpMailer {
    private final ObjectProvider<JavaMailSender> mailSender;
    private final String host;
    private final String from;

    public OtpMailer(ObjectProvider<JavaMailSender> mailSender,
                     @Value("${spring.mail.host:}") String host,
                     @Value("${app.mail.from:}") String from) {
        this.mailSender = mailSender;
        this.host = host;
        this.from = from;
    }

    public boolean enabled() {
        return StringUtils.hasText(host) && StringUtils.hasText(from) && mailSender.getIfAvailable() != null;
    }

    public void send(String to, String code, int ttlMinutes) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) throw new IllegalStateException("Mail not configured");
        try {
            var msg = sender.createMimeMessage();
            var h = new MimeMessageHelper(msg, false, "UTF-8");
            h.setFrom(from, "Gola Grocery");
            h.setTo(to);
            h.setSubject(code + " is your Gola Grocery OTP");
            h.setText("""
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
                """.formatted(code, ttlMinutes, ttlMinutes), true);
            sender.send(msg);
        } catch (MailException | jakarta.mail.MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("OTP email to {} failed: {}", mask(to), e.getMessage());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "EMAIL_FAILED", "Couldn't send the OTP email. Please try again in a minute.");
        }
    }

    public static String mask(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] p = email.split("@", 2);
        String name = p[0].length() <= 2 ? p[0].charAt(0) + "*" : p[0].charAt(0) + "*".repeat(Math.min(5, p[0].length() - 2)) + p[0].charAt(p[0].length() - 1);
        return name + "@" + p[1];
    }
}
