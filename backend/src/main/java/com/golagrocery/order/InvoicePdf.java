package com.golagrocery.order;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Renders the GST tax invoice as an A4 PDF (sent by email / WhatsApp and downloadable from a signed link). */
public final class InvoicePdf {
    private InvoicePdf() {}

    private static final Color GREEN = new Color(12, 131, 31);
    private static final Color LIGHT = new Color(240, 246, 240);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a").withZone(ZoneId.of("Asia/Kolkata"));

    public static byte[] render(InvoiceService.InvoiceDto inv) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();
            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, GREEN);
            Font h = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font n = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font small = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);
            Font white = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);

            PdfPTable head = new PdfPTable(new float[]{3, 2});
            head.setWidthPercentage(100);
            PdfPCell left = cell(new Phrase(nz(inv.storeName(), "Gola Grocery"), title), Element.ALIGN_LEFT, null);
            left.addElement(new Paragraph(nz(inv.storeName(), "Gola Grocery"), title));
            left.addElement(new Paragraph("Freshness Delivered to Your Doorstep.", small));
            if (notBlank(inv.storeAddress())) left.addElement(new Paragraph(inv.storeAddress(), small));
            if (notBlank(inv.supportPhone())) left.addElement(new Paragraph("Phone: " + inv.supportPhone(), small));
            if (notBlank(inv.gstin())) left.addElement(new Paragraph("GSTIN: " + inv.gstin(), small));
            head.addCell(left);
            PdfPCell right = cell(new Phrase(""), Element.ALIGN_RIGHT, null);
            Paragraph t = new Paragraph("TAX INVOICE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
            t.setAlignment(Element.ALIGN_RIGHT);
            right.addElement(t);
            for (String line : new String[]{"Invoice: " + inv.invoiceNumber(), "Order: " + inv.orderNumber(),
                    "Date: " + (inv.invoiceDate() == null ? "" : DATE.format(inv.invoiceDate())),
                    "Payment: " + inv.paymentMethod() + " (" + inv.paymentStatus() + ")"}) {
                Paragraph p = new Paragraph(line, n);
                p.setAlignment(Element.ALIGN_RIGHT);
                right.addElement(p);
            }
            head.addCell(right);
            doc.add(head);

            Paragraph bill = new Paragraph();
            bill.setSpacingBefore(12);
            bill.add(new Phrase("Bill / Ship to: ", h));
            bill.add(new Phrase(nz(inv.customerName(), "") + ", " + nz(inv.customerPhone(), "") + "\n" + nz(inv.shippingAddress(), ""), n));
            doc.add(bill);

            PdfPTable items = new PdfPTable(new float[]{0.6f, 5, 1, 1.6f, 1, 1.8f});
            items.setWidthPercentage(100);
            items.setSpacingBefore(12);
            items.setHeaderRows(1);
            for (String c : new String[]{"#", "Item", "Qty", "Rate", "GST", "Amount"}) {
                PdfPCell hc = cell(new Phrase(c, white), Element.ALIGN_LEFT, GREEN);
                hc.setPadding(5);
                items.addCell(hc);
            }
            int i = 1;
            for (OrderDtos.ItemDto it : inv.items()) {
                Color bg = i % 2 == 0 ? LIGHT : null;
                items.addCell(bordered(String.valueOf(i++), n, Element.ALIGN_LEFT, bg));
                items.addCell(bordered(it.productName() + " (" + it.variantLabel() + ")", n, Element.ALIGN_LEFT, bg));
                items.addCell(bordered(String.valueOf(it.quantity()), n, Element.ALIGN_RIGHT, bg));
                items.addCell(bordered(rs(it.unitPrice()), n, Element.ALIGN_RIGHT, bg));
                items.addCell(bordered(it.gstRate().stripTrailingZeros().toPlainString() + "%", n, Element.ALIGN_RIGHT, bg));
                items.addCell(bordered(rs(it.lineTotal()), n, Element.ALIGN_RIGHT, bg));
            }
            doc.add(items);

            PdfPTable totals = new PdfPTable(new float[]{4, 1.8f});
            totals.setWidthPercentage(55);
            totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totals.setSpacingBefore(8);
            row(totals, "Item total", rs(inv.itemTotal()), n);
            if (inv.couponDiscount() != null && inv.couponDiscount().signum() > 0) row(totals, "Coupon discount", "- " + rs(inv.couponDiscount()), n);
            row(totals, "Delivery fee", rs(inv.deliveryFee()), n);
            row(totals, "Handling fee", rs(inv.handlingFee()), n);
            row(totals, "Grand total (incl. GST)", rs(inv.grandTotal()), h);
            doc.add(totals);

            if (inv.taxes() != null && !inv.taxes().isEmpty()) {
                PdfPTable tax = new PdfPTable(new float[]{1, 2, 1.5f, 1.5f});
                tax.setWidthPercentage(70);
                tax.setHorizontalAlignment(Element.ALIGN_LEFT);
                tax.setSpacingBefore(14);
                for (String c : new String[]{"GST rate", "Taxable value", "CGST", "SGST"}) tax.addCell(bordered(c, h, Element.ALIGN_LEFT, LIGHT));
                for (InvoiceService.TaxLine tl : inv.taxes()) {
                    tax.addCell(bordered(tl.gstRate().stripTrailingZeros().toPlainString() + "%", n, Element.ALIGN_LEFT, null));
                    tax.addCell(bordered(rs(tl.taxableValue()), n, Element.ALIGN_RIGHT, null));
                    tax.addCell(bordered(rs(tl.cgst()), n, Element.ALIGN_RIGHT, null));
                    tax.addCell(bordered(rs(tl.sgst()), n, Element.ALIGN_RIGHT, null));
                }
                doc.add(tax);
            }
            Paragraph foot = new Paragraph("Prices include GST. This is a computer-generated invoice and needs no signature. "
                + "Thank you for shopping with Gola Grocery!", small);
            foot.setSpacingBefore(18);
            doc.add(foot);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Could not render invoice PDF: " + e.getMessage(), e);
        }
    }

    private static PdfPCell cell(Phrase p, int align, Color bg) {
        PdfPCell c = new PdfPCell(p);
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(align);
        if (bg != null) c.setBackgroundColor(bg);
        return c;
    }

    private static PdfPCell bordered(String text, Font f, int align, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setHorizontalAlignment(align);
        c.setPadding(4);
        c.setBorderColor(new Color(220, 224, 218));
        if (bg != null) c.setBackgroundColor(bg);
        return c;
    }

    private static void row(PdfPTable t, String label, String value, Font f) {
        t.addCell(cell(new Phrase(label, f), Element.ALIGN_LEFT, null));
        t.addCell(cell(new Phrase(value, f), Element.ALIGN_RIGHT, null));
    }

    static String rs(BigDecimal v) { return "Rs. " + (v == null ? "0.00" : v.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()); }
    private static String nz(String s, String d) { return s == null || s.isBlank() ? d : s; }
    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }
}
