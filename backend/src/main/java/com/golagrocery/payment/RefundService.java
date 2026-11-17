package com.golagrocery.payment;

import com.golagrocery.audit.AuditService;
import com.golagrocery.common.ApiException;
import com.golagrocery.common.Money;
import com.golagrocery.notification.NotificationService;
import com.golagrocery.order.Order;
import com.golagrocery.order.OrderRepository;
import com.golagrocery.order.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {
    private final RefundRepository refunds;
    private final PaymentRepository payments;
    private final OrderRepository orders;
    private final PaymentGateway gateway;
    private final NotificationService notifications;
    private final AuditService audit;

    /** Creates a refund request. Amount is capped at (paid amount - refunds already requested). */
    @Transactional
    public Refund request(Long orderId, BigDecimal amount, String reason, Long requestedBy) {
        Order o = orders.findById(orderId).orElseThrow(() -> ApiException.notFound("Order"));
        if (o.getPaymentStatus() != PaymentStatus.PAID && o.getPaymentStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw ApiException.badRequest("Only paid orders can be refunded");
        }
        BigDecimal already = refunds.sumActiveForOrder(orderId);
        BigDecimal refundable = o.getGrandTotal().subtract(already);
        if (amount == null) amount = refundable;
        if (amount.signum() <= 0 || amount.compareTo(refundable) > 0) {
            throw ApiException.badRequest("Refund amount must be between ₹0.01 and ₹" + refundable);
        }
        Refund r = new Refund();
        r.setOrderId(orderId);
        r.setPaymentId(payments.findFirstByOrderIdAndStatusOrderByIdDesc(orderId, "PAID")
            .or(() -> payments.findFirstByOrderIdAndStatusOrderByIdDesc(orderId, "COD_COLLECTED"))
            .map(Payment::getId).orElse(null));
        r.setAmount(Money.round(amount));
        r.setReason(reason);
        r.setStatus("REQUESTED");
        r.setRequestedBy(requestedBy);
        refunds.save(r);
        audit.log("REFUND_REQUESTED", "Order", orderId, "amount=" + r.getAmount());
        return r;
    }

    /** Approves and executes a refund through the gateway (or marks a manual refund for COD). */
    @Transactional(noRollbackFor = ApiException.class)
    public Refund process(Long refundId, Long adminId) {
        Refund r = refunds.findById(refundId).orElseThrow(() -> ApiException.notFound("Refund"));
        if (!"REQUESTED".equals(r.getStatus()) && !"FAILED".equals(r.getStatus())) {
            throw ApiException.badRequest("Refund is already " + r.getStatus());
        }
        Payment p = r.getPaymentId() == null ? null : payments.findById(r.getPaymentId()).orElse(null);
        Order o = orders.findById(r.getOrderId()).orElseThrow();
        r.setProcessedBy(adminId);
        if (p != null && ("RAZORPAY".equals(p.getProvider()) || "MOCK".equals(p.getProvider())) && p.getProviderPaymentId() != null) {
            try {
                var res = gateway.refund(p.getProviderPaymentId(), Money.toPaise(r.getAmount()), r.getReason());
                r.setProviderRefundId(res.refundId());
                r.setStatus(res.processed() ? "PROCESSED" : "APPROVED");
            } catch (IllegalStateException e) {
                r.setStatus("FAILED");
                refunds.save(r);
                throw ApiException.conflict(e.getMessage());
            }
        } else {
            // COD / manual: the store returns money via cash/UPI outside the gateway. Recorded as PROCESSED by an admin.
            r.setProviderRefundId("MANUAL");
            r.setStatus("PROCESSED");
        }
        refunds.save(r);
        BigDecimal total = refunds.sumActiveForOrder(o.getId());
        o.setPaymentStatus(total.compareTo(o.getGrandTotal()) >= 0 ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED);
        orders.save(o);
        audit.log("REFUND_" + r.getStatus(), "Order", o.getId(), "refundId=" + r.getId() + " amount=" + r.getAmount());
        try {
            notifications.notifyUser(o.getUserId(), "Refund initiated",
                "₹" + r.getAmount() + " for order #" + o.getOrderNumber() + " has been initiated. It may take 5-7 working days to reflect.",
                "ORDER", String.valueOf(o.getId()));
        } catch (RuntimeException e) {
            log.warn("Refund notification failed: {}", e.getMessage());
        }
        return r;
    }

    @Transactional
    public Refund reject(Long refundId, Long adminId, String note) {
        Refund r = refunds.findById(refundId).orElseThrow(() -> ApiException.notFound("Refund"));
        if (!"REQUESTED".equals(r.getStatus())) throw ApiException.badRequest("Only requested refunds can be rejected");
        r.setStatus("REJECTED");
        r.setProcessedBy(adminId);
        if (note != null) r.setReason(r.getReason() + " | Rejected: " + note);
        audit.log("REFUND_REJECTED", "Order", r.getOrderId(), "refundId=" + r.getId());
        return refunds.save(r);
    }
}
