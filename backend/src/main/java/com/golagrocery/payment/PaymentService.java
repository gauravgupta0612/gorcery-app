package com.golagrocery.payment;

import com.golagrocery.audit.AuditService;
import com.golagrocery.common.ApiException;
import com.golagrocery.common.Money;
import com.golagrocery.config.AppProperties;
import com.golagrocery.order.*;
import com.golagrocery.order.OrderDtos.PaymentInit;
import com.golagrocery.user.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Payment lifecycle. An order only becomes PLACED after the gateway confirms payment
 * (verified signature or verified webhook). Nothing is marked paid on the client's word.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentGateway gateway;
    private final PaymentRepository payments;
    private final OrderRepository orders;
    private final OrderNotifier notifier;
    private final AuditService audit;
    private final AppProperties props;
    private final ObjectMapper mapper;

    /** Creates a gateway order for an online-payment order and records a CREATED payment. */
    @Transactional
    public PaymentInit initiate(Order order, User customer) {
        String providerOrderId = gateway.createOrder(order.getOrderNumber(), Money.toPaise(order.getGrandTotal()));
        Payment p = new Payment();
        p.setOrderId(order.getId());
        p.setProvider(gateway.provider());
        p.setMethod(order.getPaymentMethod().name());
        p.setAmount(order.getGrandTotal());
        p.setStatus("CREATED");
        p.setProviderOrderId(providerOrderId);
        payments.save(p);
        return new PaymentInit(gateway.provider(), gateway.publicKey(), providerOrderId, Money.toPaise(order.getGrandTotal()), "INR",
            order.getOrderNumber(), gateway.testMode(), customer.getFullName(), customer.getPhone(), customer.getEmail());
    }

    @Transactional
    public void recordCod(Order order) {
        Payment p = new Payment();
        p.setOrderId(order.getId());
        p.setProvider("COD");
        p.setMethod("COD");
        p.setAmount(order.getGrandTotal());
        p.setStatus("COD_PENDING");
        payments.save(p);
    }

    /** Client-side callback after Razorpay checkout succeeds. Signature is verified server-side. */
    @Transactional
    public Order verifyRazorpay(Long userId, Long orderId, String providerOrderId, String providerPaymentId, String signature) {
        Order o = orders.findByIdAndUserId(orderId, userId).orElseThrow(() -> ApiException.notFound("Order"));
        Payment p = payments.findByProviderOrderId(providerOrderId)
            .filter(x -> x.getOrderId().equals(o.getId()))
            .orElseThrow(() -> ApiException.badRequest("Payment does not belong to this order"));
        if ("PAID".equals(p.getStatus())) return o; // idempotent
        if (!"RAZORPAY".equals(p.getProvider()) || !gateway.verifyPaymentSignature(providerOrderId, providerPaymentId, signature)) {
            audit.logAs(userId, "CUSTOMER", "PAYMENT_SIGNATURE_INVALID", "Order", o.getId(), providerOrderId);
            throw ApiException.badRequest("Payment verification failed. If money was debited it will be auto-refunded by your bank.");
        }
        p.setProviderPaymentId(providerPaymentId);
        p.setProviderSignature(signature);
        markPaid(o, p, "Payment verified (Razorpay)");
        return o;
    }

    /** Local development only: completes or fails a MOCK payment. Disabled when PAYMENTS_MODE=razorpay. */
    @Transactional
    public Order completeMock(Long userId, Long orderId, boolean success) {
        if (props.payments().isRazorpay() || !"MOCK".equals(gateway.provider())) {
            throw ApiException.forbidden("Mock payments are disabled");
        }
        Order o = orders.findByIdAndUserId(orderId, userId).orElseThrow(() -> ApiException.notFound("Order"));
        Payment p = payments.findFirstByOrderIdOrderByIdDesc(o.getId()).orElseThrow(() -> ApiException.notFound("Payment"));
        if (!"CREATED".equals(p.getStatus()) && !"FAILED".equals(p.getStatus())) return o;
        if (success) {
            p.setProviderPaymentId("mock_pay_" + com.golagrocery.common.Crypto.randomToken(9));
            markPaid(o, p, "TEST payment completed (mock gateway - no money collected)");
        } else {
            p.setStatus("FAILED");
            p.setFailureReason("Declined in test mode");
            payments.save(p);
        }
        return o;
    }

    /** Called when the customer dismisses/fails checkout. The order stays PENDING_PAYMENT so they can retry. */
    @Transactional
    public void reportFailure(Long userId, Long orderId, String reason) {
        Order o = orders.findByIdAndUserId(orderId, userId).orElseThrow(() -> ApiException.notFound("Order"));
        payments.findFirstByOrderIdOrderByIdDesc(o.getId()).filter(p -> "CREATED".equals(p.getStatus())).ifPresent(p -> {
            p.setStatus("FAILED");
            p.setFailureReason(reason == null ? "Cancelled by user" : reason.substring(0, Math.min(reason.length(), 300)));
            payments.save(p);
        });
    }

    @Transactional
    public PaymentInit retry(Long userId, Long orderId, User customer) {
        Order o = orders.findByIdAndUserId(orderId, userId).orElseThrow(() -> ApiException.notFound("Order"));
        if (o.getStatus() != OrderStatus.PENDING_PAYMENT) throw ApiException.badRequest("This order does not need payment");
        return initiate(o, customer);
    }

    /** Razorpay webhook (payment.captured / payment.failed). Signature verified with the webhook secret. */
    @Transactional
    public void handleWebhook(String body, String signature) {
        if (!(gateway instanceof RazorpayGateway rz) || !rz.verifyWebhookSignature(body, signature)) {
            throw ApiException.unauthorized("Invalid webhook signature");
        }
        try {
            JsonNode root = mapper.readTree(body);
            String event = root.path("event").asText();
            JsonNode entity = root.path("payload").path("payment").path("entity");
            String providerOrderId = entity.path("order_id").asText(null);
            if (providerOrderId == null) return;
            payments.findByProviderOrderId(providerOrderId).ifPresent(p -> {
                Order o = orders.findById(p.getOrderId()).orElse(null);
                if (o == null) return;
                if ("payment.captured".equals(event) && !"PAID".equals(p.getStatus())) {
                    p.setProviderPaymentId(entity.path("id").asText());
                    markPaid(o, p, "Payment captured (Razorpay webhook)");
                } else if ("payment.failed".equals(event) && "CREATED".equals(p.getStatus())) {
                    p.setStatus("FAILED");
                    p.setFailureReason(entity.path("error_description").asText("Payment failed"));
                    payments.save(p);
                }
            });
        } catch (java.io.IOException e) {
            throw ApiException.badRequest("Invalid webhook payload");
        }
    }

    private void markPaid(Order o, Payment p, String note) {
        p.setStatus("PAID");
        payments.save(p);
        if (o.getStatus() == OrderStatus.PENDING_PAYMENT) {
            o.setStatus(OrderStatus.PLACED);
            o.setPaymentStatus(PaymentStatus.PAID);
            o.addHistory(OrderStatus.PLACED, note, null);
            orders.save(o);
            notifier.statusChanged(o);
        } else if (o.getStatus() == OrderStatus.PAYMENT_FAILED || o.getStatus() == OrderStatus.CANCELLED) {
            // Payment arrived after the order expired: record it so an admin refunds it from Refund Management.
            o.setPaymentStatus(PaymentStatus.PAID);
            o.addHistory(o.getStatus(), "Late payment received - refund required", null);
            orders.save(o);
            log.warn("Late payment for expired order {} - needs refund", o.getOrderNumber());
        }
        audit.log("PAYMENT_CONFIRMED", "Order", o.getId(), p.getProvider() + " " + p.getAmount());
    }
}
