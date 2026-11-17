package com.golagrocery.order;

import com.golagrocery.notification.WhatsAppSender;
import com.golagrocery.user.OtpMailer;
import com.golagrocery.user.User;
import com.golagrocery.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Sends the tax invoice to the customer once an order is placed (COD) or paid (online):
 *  - email with the PDF attached (free, via the Gmail relay / SMTP),
 *  - WhatsApp document message (when the WhatsApp Business Cloud API is configured).
 * Runs in the background after the order is committed; failures are logged and never affect the order.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceSender {
    private final InvoiceService invoices;
    private final InvoiceLinks links;
    private final UserRepository users;
    private final OtpMailer mailer;
    private final WhatsAppSender whatsApp;

    @Async
    public void send(Long orderId, Long userId) {
        InvoiceService.InvoiceDto inv;
        byte[] pdf;
        try {
            inv = invoices.forAdmin(orderId);
            pdf = InvoicePdf.render(inv);
        } catch (RuntimeException e) {
            log.warn("Invoice for order {} not sent: {}", orderId, e.getMessage());
            return;
        }
        User user = users.findById(userId).orElse(null);
        String fileName = "Invoice-" + inv.orderNumber() + ".pdf";
        String link = links.url(orderId, inv.orderNumber());
        String amount = InvoicePdf.rs(inv.grandTotal());

        String email = user == null ? null : user.getEmail();
        if (email != null && !email.isBlank() && mailer.enabled()) {
            try {
                mailer.sendDocument(email, "Your Gola Grocery invoice " + inv.invoiceNumber(), emailHtml(inv, link), fileName, pdf);
                log.info("Invoice {} emailed to {}", inv.invoiceNumber(), OtpMailer.mask(email));
            } catch (Exception e) {
                log.warn("Invoice email for {} failed: {}", inv.orderNumber(), e.getMessage());
            }
        }
        String phone = user != null && user.getPhone() != null ? user.getPhone() : inv.customerPhone();
        if (phone != null && whatsApp.enabled()) {
            try {
                whatsApp.sendInvoice(phone, inv.customerName(), inv.orderNumber(), amount, link, fileName);
                log.info("Invoice {} sent on WhatsApp to ******{}", inv.invoiceNumber(), phone.length() > 4 ? phone.substring(phone.length() - 4) : "");
            } catch (Exception e) {
                log.warn("Invoice WhatsApp for {} failed: {}", inv.orderNumber(), e.getMessage());
            }
        }
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String emailHtml(InvoiceService.InvoiceDto inv, String link) {
        StringBuilder rows = new StringBuilder();
        for (OrderDtos.ItemDto it : inv.items()) {
            rows.append("<tr><td style=\"padding:6px 0\">").append(esc(it.productName())).append(" <span style=\"color:#6b7280\">(")
                .append(esc(it.variantLabel())).append(") &times; ").append(it.quantity()).append("</span></td><td style=\"text-align:right\">")
                .append(InvoicePdf.rs(it.lineTotal())).append("</td></tr>");
        }
        return """
            <div style="font-family:Arial,sans-serif;max-width:520px;margin:auto;border:1px solid #e6e8eb;border-radius:12px;overflow:hidden">
              <div style="background:#0C831F;color:#fff;padding:16px 20px;font-size:18px;font-weight:bold">Gola Grocery</div>
              <div style="padding:20px">
                <p>Hi %s,</p>
                <p>Thank you for your order <b>%s</b>! Your tax invoice <b>%s</b> is attached as a PDF.</p>
                <table style="width:100%%;border-collapse:collapse;font-size:14px">%s
                  <tr><td style="padding-top:10px;border-top:1px solid #e6e8eb"><b>Total paid / payable</b></td>
                      <td style="padding-top:10px;border-top:1px solid #e6e8eb;text-align:right"><b>%s</b></td></tr></table>
                <p style="margin-top:16px">Payment: %s (%s)</p>
                <p><a href="%s" style="background:#F8CB46;color:#1f2937;padding:10px 16px;border-radius:8px;text-decoration:none;font-weight:bold">Download invoice</a></p>
                <p style="color:#6b7280;font-size:12px">Track your order in the Gola Grocery app. Questions? Just reply to this email.</p>
              </div>
              <div style="background:#FFF8DB;color:#0B3D16;padding:10px 20px;font-size:12px">Freshness Delivered to Your Doorstep.</div>
            </div>
            """.formatted(esc(inv.customerName()), esc(inv.orderNumber()), esc(inv.invoiceNumber()), rows, InvoicePdf.rs(inv.grandTotal()),
                esc(inv.paymentMethod()), esc(inv.paymentStatus()), link);
    }
}
