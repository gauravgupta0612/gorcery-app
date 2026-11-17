package com.golagrocery.order;

import com.golagrocery.audit.AuditService;
import com.golagrocery.cart.Cart;
import com.golagrocery.cart.CartService;
import com.golagrocery.catalog.*;
import com.golagrocery.common.ApiException;
import com.golagrocery.common.Crypto;
import com.golagrocery.common.Money;
import com.golagrocery.common.PageDto;
import com.golagrocery.delivery.DeliveryAssignmentRepository;
import com.golagrocery.delivery.DeliveryPartnerRepository;
import com.golagrocery.order.OrderDtos.*;
import com.golagrocery.order.PricingService.LineInput;
import com.golagrocery.order.PricingService.Quote;
import com.golagrocery.payment.*;
import com.golagrocery.promo.CouponRedemption;
import com.golagrocery.promo.CouponRedemptionRepository;
import com.golagrocery.promo.CouponRepository;
import com.golagrocery.review.ReviewRepository;
import com.golagrocery.store.StoreService;
import com.golagrocery.store.StoreSettings;
import com.golagrocery.user.Address;
import com.golagrocery.user.AddressRepository;
import com.golagrocery.user.User;
import com.golagrocery.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orders;
    private final CartService cartService;
    private final PricingService pricing;
    private final InventoryRepository inventory;
    private final InventoryMovementRepository movements;
    private final ProductVariantRepository variants;
    private final AddressRepository addresses;
    private final UserRepository users;
    private final StoreService store;
    private final PaymentService paymentService;
    private final PaymentRepository payments;
    private final RefundService refundService;
    private final RefundRepository refunds;
    private final CouponRepository coupons;
    private final CouponRedemptionRepository redemptions;
    private final DeliveryAssignmentRepository assignments;
    private final DeliveryPartnerRepository partners;
    private final ReviewRepository reviews;
    private final OrderNotifier notifier;
    private final AuditService audit;

    // ------------------------------------------------------------------ checkout
    @Transactional
    public CheckoutResponse checkout(Long userId, CheckoutRequest req) {
        StoreSettings s = store.settings();
        if (!store.isOpenNow(s)) {
            throw ApiException.conflict("The store is closed right now. We deliver between " + s.getOpeningTime() + " and " + s.getClosingTime() + ".");
        }
        User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        Address addr = addresses.findByIdAndUserId(req.addressId(), userId).orElseThrow(() -> ApiException.notFound("Address"));
        if (store.serviceable(addr.getPincode()).isEmpty()) {
            throw ApiException.badRequest("Sorry, we don't deliver to pincode " + addr.getPincode() + " yet");
        }
        Cart cart = cartService.getOrCreate(userId);
        if (cart.getItems().isEmpty()) throw ApiException.badRequest("Your cart is empty");
        Quote q = pricing.quote(cart.getItems().stream().map(i -> new LineInput(i.getVariant(), i.getQuantity())).toList(),
            userId, cart.getCouponCode(), addr.getPincode());
        if (q.hasProblems()) throw ApiException.conflict("Some items in your cart are unavailable. Please review your cart.");
        if (!q.meetsMinimum()) throw ApiException.badRequest("Minimum order value is ₹" + q.minOrderAmount());
        if (cart.getCouponCode() != null && !q.couponValid()) {
            throw ApiException.badRequest("Coupon " + cart.getCouponCode() + " can't be applied: " + q.couponMessage() + ". Please remove it.");
        }
        if (req.paymentMethod() == PaymentMethod.COD) {
            if (!s.isCodEnabled()) throw ApiException.badRequest("Cash on Delivery is currently unavailable");
            if (q.grandTotal().compareTo(s.getCodMaxAmount()) > 0) {
                throw ApiException.badRequest("Cash on Delivery is available for orders up to ₹" + s.getCodMaxAmount());
            }
        }

        Order o = new Order();
        o.setOrderNumber("GG" + orders.nextOrderNumber());
        o.setUserId(userId);
        o.setPaymentMethod(req.paymentMethod());
        boolean online = req.paymentMethod().isOnline();
        o.setStatus(online ? OrderStatus.PENDING_PAYMENT : OrderStatus.PLACED);
        o.setPaymentStatus(online ? PaymentStatus.PENDING : PaymentStatus.COD_PENDING);
        o.setShipName(addr.getContactName());
        o.setShipPhone(addr.getContactPhone());
        o.setShipLine1(addr.getLine1());
        o.setShipLine2(addr.getLine2());
        o.setShipLandmark(addr.getLandmark());
        o.setShipCity(addr.getCity());
        o.setShipState(addr.getState());
        o.setShipPincode(addr.getPincode());
        o.setShipLat(addr.getLatitude());
        o.setShipLng(addr.getLongitude());
        o.setItemTotal(q.itemTotal());
        o.setMrpTotal(q.mrpTotal());
        o.setCouponCode(q.couponValid() ? q.couponCode() : null);
        o.setCouponDiscount(q.couponDiscount());
        o.setDeliveryFee(q.deliveryFee());
        o.setHandlingFee(q.handlingFee());
        o.setTaxTotal(q.taxTotal());
        o.setGrandTotal(q.grandTotal());
        o.setDeliveryInstructions(req.deliveryInstructions());
        o.setDeliveryOtp(Crypto.randomDigits(4));

        for (PricingService.Line l : q.lines()) {
            OrderItem it = new OrderItem();
            it.setOrder(o);
            it.setProductId(l.productId());
            it.setVariantId(l.variantId());
            it.setProductName(l.productName());
            it.setVariantLabel(l.variantLabel());
            it.setImageUrl(l.imageUrl());
            it.setSku(variants.findById(l.variantId()).map(ProductVariant::getSku).orElse(""));
            it.setUnitMrp(l.unitMrp());
            it.setUnitPrice(l.unitPrice());
            it.setQuantity(l.quantity());
            it.setGstRate(l.gstRate());
            it.setTaxAmount(l.tax());
            it.setLineTotal(l.lineTotal());
            o.getItems().add(it);
        }
        o.addHistory(o.getStatus(), online ? "Awaiting payment" : "Cash on Delivery order placed", userId);
        orders.save(o);

        // Atomic stock deduction; any failure rolls back the whole checkout.
        for (OrderItem it : o.getItems()) {
            if (inventory.tryDeduct(it.getVariantId(), it.getQuantity()) == 0) {
                throw ApiException.conflict(it.getProductName() + " (" + it.getVariantLabel() + ") just went out of stock");
            }
            recordMovement(it.getVariantId(), -it.getQuantity(), "ORDER", o.getOrderNumber(), userId);
        }
        if (q.couponId() != null) {
            CouponRedemption r = new CouponRedemption();
            r.setCouponId(q.couponId());
            r.setUserId(userId);
            r.setOrderId(o.getId());
            r.setAmount(q.couponDiscount());
            redemptions.save(r);
            coupons.addUsage(q.couponId(), 1);
        }

        PaymentInit init = null;
        if (online) {
            init = paymentService.initiate(o, user);
        } else {
            paymentService.recordCod(o);
            notifier.statusChanged(o);
        }
        cartService.clear(userId);
        audit.logAs(userId, "CUSTOMER", "ORDER_CREATED", "Order", o.getId(), o.getOrderNumber() + " " + o.getPaymentMethod() + " ₹" + o.getGrandTotal());
        return new CheckoutResponse(toDetail(o, true), init);
    }

    // ------------------------------------------------------------------ customer queries
    @Transactional(readOnly = true)
    public PageDto<OrderSummaryDto> myOrders(Long userId, int page, int size) {
        return PageDto.of(orders.findByUserIdOrderByPlacedAtDesc(userId, PageRequest.of(Math.max(page, 0), Math.min(size, 50))),
            OrderSummaryDto::of);
    }

    @Transactional(readOnly = true)
    public OrderDetailDto myOrder(Long userId, Long orderId) {
        return toDetail(orders.findByIdAndUserId(orderId, userId).orElseThrow(() -> ApiException.notFound("Order")), true);
    }

    @Transactional
    public OrderDetailDto cancelByCustomer(Long userId, Long orderId, String reason) {
        Order o = orders.findByIdAndUserId(orderId, userId).orElseThrow(() -> ApiException.notFound("Order"));
        if (!o.getStatus().customerCanCancel()) {
            throw ApiException.badRequest("This order can no longer be cancelled. Please contact support.");
        }
        cancel(o, "Cancelled by customer: " + reason, userId);
        return toDetail(o, true);
    }

    @Transactional
    public ReorderResponse reorder(Long userId, Long orderId) {
        Order o = orders.findByIdAndUserId(orderId, userId).orElseThrow(() -> ApiException.notFound("Order"));
        int added = 0;
        List<String> skipped = new ArrayList<>();
        for (OrderItem it : o.getItems()) {
            try {
                ProductVariant v = variants.findById(it.getVariantId()).orElse(null);
                if (v == null || !v.isActive() || !v.getProduct().isActive() || v.stock() <= 0) {
                    skipped.add(it.getProductName() + " (unavailable)");
                    continue;
                }
                int qty = Math.min(it.getQuantity(), Math.min(v.stock(), v.getMaxPerOrder()));
                cartService.setQuantity(userId, v.getId(), qty, null);
                added++;
                if (qty < it.getQuantity()) skipped.add(it.getProductName() + " (only " + qty + " added)");
            } catch (ApiException e) {
                skipped.add(it.getProductName() + " (" + e.getMessage() + ")");
            }
        }
        return new ReorderResponse(added, skipped);
    }

    // ------------------------------------------------------------------ admin
    @Transactional
    public OrderDetailDto adminUpdateStatus(Long orderId, OrderStatus next, String note, Long actorId) {
        Order o = orders.findById(orderId).orElseThrow(() -> ApiException.notFound("Order"));
        if (!o.getStatus().adminCanMoveTo(next)) {
            throw ApiException.badRequest("Cannot change status from " + o.getStatus() + " to " + next);
        }
        if (next == OrderStatus.CANCELLED) {
            cancel(o, "Cancelled by store" + (note == null || note.isBlank() ? "" : ": " + note), actorId);
        } else {
            o.setStatus(next);
            o.addHistory(next, note, actorId);
            orders.save(o);
            notifier.statusChanged(o);
            audit.log("ORDER_STATUS_" + next, "Order", o.getId(), note);
        }
        return toDetail(o, false);
    }

    @Transactional(readOnly = true)
    public OrderDetailDto adminDetail(Long orderId) {
        return toDetail(orders.findById(orderId).orElseThrow(() -> ApiException.notFound("Order")), false);
    }

    // ------------------------------------------------------------------ internals
    /** Cancels an order: restores stock, releases coupon, cancels delivery assignment, refunds online payments. */
    void cancel(Order o, String reason, Long actorId) {
        restoreStockAndCoupon(o, actorId);
        assignments.findFirstByOrderIdAndStatusNot(o.getId(), "CANCELLED").ifPresent(a -> {
            a.setStatus("CANCELLED");
            assignments.save(a);
        });
        o.setStatus(OrderStatus.CANCELLED);
        o.setCancelReason(reason.length() > 300 ? reason.substring(0, 300) : reason);
        o.addHistory(OrderStatus.CANCELLED, o.getCancelReason(), actorId);
        payments.findFirstByOrderIdOrderByIdDesc(o.getId()).ifPresent(p -> {
            if ("CREATED".equals(p.getStatus()) || "COD_PENDING".equals(p.getStatus())) {
                p.setStatus("FAILED");
                p.setFailureReason("Order cancelled");
                payments.save(p);
            }
        });
        orders.save(o);
        if (o.getPaymentStatus() == PaymentStatus.PAID) {
            Refund r = refundService.request(o.getId(), null, "Order cancelled", actorId);
            try {
                refundService.process(r.getId(), actorId);
            } catch (ApiException e) {
                log.warn("Automatic refund for {} failed - left for admin: {}", o.getOrderNumber(), e.getMessage());
            }
        } else if (o.getPaymentStatus() == PaymentStatus.PENDING || o.getPaymentStatus() == PaymentStatus.COD_PENDING) {
            o.setPaymentStatus(PaymentStatus.FAILED);
            orders.save(o);
        }
        notifier.statusChanged(o);
        audit.log("ORDER_CANCELLED", "Order", o.getId(), reason);
    }

    private void restoreStockAndCoupon(Order o, Long actorId) {
        for (OrderItem it : o.getItems()) {
            inventory.restore(it.getVariantId(), it.getQuantity());
            recordMovement(it.getVariantId(), it.getQuantity(), "CANCEL", o.getOrderNumber(), actorId);
        }
        redemptions.findByOrderId(o.getId()).ifPresent(r -> {
            coupons.addUsage(r.getCouponId(), -1);
            redemptions.delete(r);
        });
    }

    private void recordMovement(Long variantId, int delta, String reason, String ref, Long by) {
        InventoryMovement m = new InventoryMovement();
        m.setVariantId(variantId);
        m.setDelta(delta);
        m.setReason(reason);
        m.setReference(ref);
        m.setCreatedBy(by);
        movements.save(m);
    }

    /** Online orders not paid within 30 minutes are marked PAYMENT_FAILED and their stock is released. */
    @Scheduled(fixedDelay = 5, timeUnit = java.util.concurrent.TimeUnit.MINUTES)
    @Transactional
    public void expireUnpaidOrders() {
        for (Order o : orders.findByStatusAndPlacedAtBefore(OrderStatus.PENDING_PAYMENT, Instant.now().minus(30, ChronoUnit.MINUTES))) {
            restoreStockAndCoupon(o, null);
            o.setStatus(OrderStatus.PAYMENT_FAILED);
            o.setPaymentStatus(PaymentStatus.FAILED);
            o.addHistory(OrderStatus.PAYMENT_FAILED, "Payment not completed within 30 minutes", null);
            orders.save(o);
            notifier.statusChanged(o);
            log.info("Expired unpaid order {}", o.getOrderNumber());
        }
    }

    public OrderDetailDto toDetail(Order o, boolean forCustomer) {
        PartnerDto partner = assignments.findFirstByOrderIdAndStatusNot(o.getId(), "CANCELLED")
            .flatMap(a -> partners.findById(a.getDeliveryPartnerId()))
            .flatMap(p -> users.findById(p.getUserId()).map(u -> new PartnerDto(p.getId(), u.getFullName(), u.getPhone(),
                p.getVehicleType(), p.getVehicleNumber())))
            .orElse(null);
        var payment = payments.findFirstByOrderIdOrderByIdDesc(o.getId()).orElse(null);
        boolean showOtp = forCustomer && Set.of(OrderStatus.PACKED, OrderStatus.ASSIGNED, OrderStatus.OUT_FOR_DELIVERY,
            OrderStatus.CONFIRMED, OrderStatus.PREPARING, OrderStatus.PLACED).contains(o.getStatus());
        boolean canReview = forCustomer && o.getStatus() == OrderStatus.DELIVERED
            && o.getItems().stream().anyMatch(i -> !reviews.existsByProductIdAndUserId(i.getProductId(), o.getUserId()));
        return new OrderDetailDto(o.getId(), o.getOrderNumber(), o.getStatus().name(), o.getPaymentMethod().name(),
            o.getPaymentStatus().name(), payment == null ? null : payment.getProvider(),
            payment != null && "MOCK".equals(payment.getProvider()),
            o.getItems().stream().map(ItemDto::of).toList(), AddressSnapshot.of(o), o.getItemTotal(), o.getMrpTotal(),
            o.getCouponCode(), o.getCouponDiscount(), o.getDeliveryFee(), o.getHandlingFee(), o.getTaxTotal(), o.getGrandTotal(),
            o.getDeliveryInstructions(), o.getCancelReason(), showOtp && o.getStatus().isActive() ? o.getDeliveryOtp() : null,
            o.getPlacedAt(), o.getDeliveredAt(),
            o.getHistory().stream().map(h -> new HistoryDto(h.getStatus(), h.getNote(), h.getCreatedAt())).toList(),
            partner, forCustomer && o.getStatus().customerCanCancel(), forCustomer && o.getStatus() == OrderStatus.PENDING_PAYMENT,
            canReview,
            refunds.findByOrderIdOrderByIdDesc(o.getId()).stream()
                .map(r -> new RefundDto(r.getId(), r.getAmount(), r.getReason(), r.getStatus(), r.getCreatedAt())).toList(),
            forCustomer ? null : o.getUserId());
    }

    public static java.math.BigDecimal zeroIfNull(java.math.BigDecimal v) { return v == null ? java.math.BigDecimal.ZERO : Money.round(v); }
}
