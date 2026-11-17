package com.golagrocery.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Sends WhatsApp messages through Meta's official WhatsApp Business Cloud API.
 * Disabled until WHATSAPP_TOKEN and WHATSAPP_PHONE_NUMBER_ID are set.
 *
 * The invoice uses an approved "Utility" template (default name: order_invoice, language en) with
 *   - header: DOCUMENT
 *   - body:   "Hi {{1}}, thank you for shopping with Gola Grocery! Your invoice for order {{2}} ({{3}}) is attached."
 */
@Slf4j
@Component
public class WhatsAppSender {
    private final String token;
    private final String phoneNumberId;
    private final String template;
    private final String language;
    private final String apiVersion;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public WhatsAppSender(@Value("${app.whatsapp.token:}") String token,
                          @Value("${app.whatsapp.phone-number-id:}") String phoneNumberId,
                          @Value("${app.whatsapp.invoice-template:order_invoice}") String template,
                          @Value("${app.whatsapp.language:en}") String language,
                          @Value("${app.whatsapp.api-version:v21.0}") String apiVersion) {
        this.token = token;
        this.phoneNumberId = phoneNumberId;
        this.template = template;
        this.language = language;
        this.apiVersion = apiVersion;
        if (!enabled()) log.info("WhatsApp: not configured (set WHATSAPP_TOKEN and WHATSAPP_PHONE_NUMBER_ID to send invoices on WhatsApp)");
    }

    public boolean enabled() { return StringUtils.hasText(token) && StringUtils.hasText(phoneNumberId); }

    /** Sends the invoice template with the PDF as a document header. Phone: 10-digit Indian number or full E.164 digits. */
    public void sendInvoice(String phone, String customerName, String orderNumber, String amount, String pdfUrl, String fileName) throws Exception {
        if (!enabled()) throw new IllegalStateException("WhatsApp is not configured");
        String to = phone.replaceAll("\\D", "");
        if (to.length() == 10) to = "91" + to;
        Map<String, Object> payload = Map.of(
            "messaging_product", "whatsapp",
            "to", to,
            "type", "template",
            "template", Map.of(
                "name", template,
                "language", Map.of("code", language),
                "components", List.of(
                    Map.of("type", "header", "parameters", List.of(
                        Map.of("type", "document", "document", Map.of("link", pdfUrl, "filename", fileName)))),
                    Map.of("type", "body", "parameters", List.of(
                        Map.of("type", "text", "text", customerName == null || customerName.isBlank() ? "there" : customerName),
                        Map.of("type", "text", "text", orderNumber),
                        Map.of("type", "text", "text", amount))))));
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://graph.facebook.com/" + apiVersion + "/" + phoneNumberId + "/messages"))
            .timeout(Duration.ofSeconds(30))
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload))).build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            String body = resp.body();
            throw new IllegalStateException("WhatsApp API HTTP " + resp.statusCode() + ": " + (body.length() > 300 ? body.substring(0, 300) : body));
        }
    }
}
