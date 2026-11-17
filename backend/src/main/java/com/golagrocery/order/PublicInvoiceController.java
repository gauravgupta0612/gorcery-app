package com.golagrocery.order;

import com.golagrocery.common.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Signed, login-free invoice PDF download (link is sent to the customer by email / WhatsApp). */
@RestController
@RequestMapping("/api/v1/public/invoices")
@RequiredArgsConstructor
public class PublicInvoiceController {
    private final InvoiceService invoices;
    private final InvoiceLinks links;

    @GetMapping("/{orderId}/{token}/{fileName:.+}")
    public ResponseEntity<byte[]> pdf(@PathVariable Long orderId, @PathVariable String token, @PathVariable String fileName) {
        if (!links.valid(orderId, token)) throw ApiException.notFound("Invoice");
        InvoiceService.InvoiceDto inv = invoices.forAdmin(orderId);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"Invoice-" + inv.orderNumber() + ".pdf\"")
            .cacheControl(CacheControl.noStore())
            .body(InvoicePdf.render(inv));
    }


}
