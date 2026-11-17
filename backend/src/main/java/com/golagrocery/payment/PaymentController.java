package com.golagrocery.payment;

import com.golagrocery.order.OrderDtos.OrderDetailDto;
import com.golagrocery.order.OrderService;
import com.golagrocery.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService payments;
    private final OrderService orders;

    public record VerifyRequest(@NotNull Long orderId, @NotBlank String razorpayOrderId, @NotBlank String razorpayPaymentId,
                                @NotBlank String razorpaySignature) {}
    public record MockCompleteRequest(@NotNull Long orderId, boolean success) {}
    public record FailureRequest(@NotNull Long orderId, @Size(max = 300) String reason) {}

    @PostMapping("/razorpay/verify")
    public OrderDetailDto verify(@Valid @RequestBody VerifyRequest r) {
        Long uid = CurrentUser.id();
        payments.verifyRazorpay(uid, r.orderId(), r.razorpayOrderId(), r.razorpayPaymentId(), r.razorpaySignature());
        return orders.myOrder(uid, r.orderId());
    }

    @PostMapping("/mock/complete")
    public OrderDetailDto mockComplete(@Valid @RequestBody MockCompleteRequest r) {
        Long uid = CurrentUser.id();
        payments.completeMock(uid, r.orderId(), r.success());
        return orders.myOrder(uid, r.orderId());
    }

    @PostMapping("/failure")
    public OrderDetailDto failure(@Valid @RequestBody FailureRequest r) {
        Long uid = CurrentUser.id();
        payments.reportFailure(uid, r.orderId(), r.reason());
        return orders.myOrder(uid, r.orderId());
    }

    @PostMapping("/razorpay/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String body,
                                        @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        payments.handleWebhook(body, signature);
        return ResponseEntity.ok().build();
    }
}
