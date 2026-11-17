package com.golagrocery.order;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {
    private OrderDtos() {}

    public record CheckoutRequest(@NotNull Long addressId, @NotNull PaymentMethod paymentMethod,
                                  @Size(max = 300) String deliveryInstructions) {}

    public record CancelRequest(@NotBlank @Size(max = 300) String reason) {}

    public record PaymentInit(String provider, String keyId, String providerOrderId, long amountPaise, String currency,
                              String orderNumber, boolean testMode, String customerName, String customerPhone, String customerEmail) {}

    public record ItemDto(Long productId, Long variantId, String productName, String variantLabel, String imageUrl, String sku,
                          BigDecimal unitMrp, BigDecimal unitPrice, int quantity, BigDecimal lineTotal, BigDecimal gstRate,
                          BigDecimal taxAmount) {
        public static ItemDto of(OrderItem i) {
            return new ItemDto(i.getProductId(), i.getVariantId(), i.getProductName(), i.getVariantLabel(), i.getImageUrl(), i.getSku(),
                i.getUnitMrp(), i.getUnitPrice(), i.getQuantity(), i.getLineTotal(), i.getGstRate(), i.getTaxAmount());
        }
    }

    public record AddressSnapshot(String name, String phone, String line1, String line2, String landmark, String city,
                                  String state, String pincode, Double lat, Double lng) {
        public static AddressSnapshot of(Order o) {
            return new AddressSnapshot(o.getShipName(), o.getShipPhone(), o.getShipLine1(), o.getShipLine2(), o.getShipLandmark(),
                o.getShipCity(), o.getShipState(), o.getShipPincode(), o.getShipLat(), o.getShipLng());
        }
    }

    public record HistoryDto(String status, String note, Instant at) {}

    public record PartnerDto(Long id, String name, String phone, String vehicleType, String vehicleNumber) {}

    public record RefundDto(Long id, BigDecimal amount, String reason, String status, Instant createdAt) {}

    public record OrderSummaryDto(Long id, String orderNumber, String status, String paymentMethod, String paymentStatus,
                                  BigDecimal grandTotal, int itemCount, Instant placedAt, List<String> images) {
        public static OrderSummaryDto of(Order o) {
            return new OrderSummaryDto(o.getId(), o.getOrderNumber(), o.getStatus().name(), o.getPaymentMethod().name(),
                o.getPaymentStatus().name(), o.getGrandTotal(), o.getItems().stream().mapToInt(OrderItem::getQuantity).sum(),
                o.getPlacedAt(), o.getItems().stream().map(OrderItem::getImageUrl).filter(java.util.Objects::nonNull).limit(4).toList());
        }
    }

    public record OrderDetailDto(Long id, String orderNumber, String status, String paymentMethod, String paymentStatus,
                                 String paymentProvider, boolean testPayment, List<ItemDto> items, AddressSnapshot address,
                                 BigDecimal itemTotal, BigDecimal mrpTotal, String couponCode, BigDecimal couponDiscount,
                                 BigDecimal deliveryFee, BigDecimal handlingFee, BigDecimal taxTotal, BigDecimal grandTotal,
                                 String deliveryInstructions, String cancelReason, String deliveryOtp, Instant placedAt,
                                 Instant deliveredAt, List<HistoryDto> history, PartnerDto deliveryPartner,
                                 boolean canCancel, boolean canRetryPayment, boolean canReview, List<RefundDto> refunds,
                                 Long customerId) {}

    public record CheckoutResponse(OrderDetailDto order, PaymentInit payment) {}

    public record ReorderResponse(int added, List<String> skipped) {}
}
