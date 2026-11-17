package com.golagrocery.order;

import com.golagrocery.common.ApiException;
import com.golagrocery.common.Money;
import com.golagrocery.store.StoreService;
import com.golagrocery.store.StoreSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Tax invoice data (GST split into CGST + SGST for intra-state supply). Rendered by the admin dashboard / app. */
@Service
@RequiredArgsConstructor
public class InvoiceService {
    private final OrderRepository orders;
    private final StoreService store;

    public record TaxLine(BigDecimal gstRate, BigDecimal taxableValue, BigDecimal cgst, BigDecimal sgst) {}

    public record InvoiceDto(String invoiceNumber, Instant invoiceDate, String storeName, String storeAddress, String gstin,
                             String supportPhone, String orderNumber, String customerName, String customerPhone, String shippingAddress,
                             String paymentMethod, String paymentStatus, List<OrderDtos.ItemDto> items, List<TaxLine> taxes,
                             BigDecimal itemTotal, BigDecimal couponDiscount, BigDecimal deliveryFee, BigDecimal handlingFee,
                             BigDecimal grandTotal, String status) {}

    @Transactional(readOnly = true)
    public InvoiceDto forCustomer(Long userId, Long orderId) {
        return build(orders.findByIdAndUserId(orderId, userId).orElseThrow(() -> ApiException.notFound("Order")));
    }

    @Transactional(readOnly = true)
    public InvoiceDto forAdmin(Long orderId) {
        return build(orders.findById(orderId).orElseThrow(() -> ApiException.notFound("Order")));
    }

    private InvoiceDto build(Order o) {
        if (o.getStatus() == OrderStatus.PENDING_PAYMENT || o.getStatus() == OrderStatus.PAYMENT_FAILED) {
            throw ApiException.badRequest("Invoice is available once the order is placed");
        }
        StoreSettings s = store.settings();
        Map<BigDecimal, BigDecimal[]> byRate = new TreeMap<>();
        for (OrderItem i : o.getItems()) {
            BigDecimal[] acc = byRate.computeIfAbsent(i.getGstRate().setScale(2), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            acc[0] = acc[0].add(i.getLineTotal().subtract(i.getTaxAmount()));
            acc[1] = acc[1].add(i.getTaxAmount());
        }
        List<TaxLine> taxes = byRate.entrySet().stream().map(e -> {
            BigDecimal half = e.getValue()[1].divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            return new TaxLine(e.getKey(), Money.round(e.getValue()[0]), half, e.getValue()[1].subtract(half));
        }).toList();
        String addr = String.join(", ", java.util.stream.Stream.of(o.getShipLine1(), o.getShipLine2(), o.getShipLandmark(),
            o.getShipCity(), o.getShipState() + " - " + o.getShipPincode()).filter(x -> x != null && !x.isBlank()).toList());
        return new InvoiceDto("INV-" + o.getOrderNumber(), o.getPlacedAt(), s.getStoreName(), s.getStoreAddress(), s.getGstin(),
            s.getSupportPhone(), o.getOrderNumber(), o.getShipName(), o.getShipPhone(), addr, o.getPaymentMethod().name(),
            o.getPaymentStatus().name(), o.getItems().stream().map(OrderDtos.ItemDto::of).toList(), taxes, o.getItemTotal(),
            o.getCouponDiscount(), o.getDeliveryFee(), o.getHandlingFee(), o.getGrandTotal(), o.getStatus().name());
    }
}
