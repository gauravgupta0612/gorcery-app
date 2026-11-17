package com.golagrocery.order;

import com.golagrocery.common.ApiException;
import com.golagrocery.common.PageDto;
import com.golagrocery.order.OrderDtos.*;
import com.golagrocery.payment.PaymentService;
import com.golagrocery.security.CurrentUser;
import com.golagrocery.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orders;
    private final PaymentService payments;
    private final UserRepository users;
    private final InvoiceService invoices;

    @PostMapping("/checkout")
    public CheckoutResponse checkout(@Valid @RequestBody CheckoutRequest req) { return orders.checkout(CurrentUser.id(), req); }

    @GetMapping
    public PageDto<OrderSummaryDto> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return orders.myOrders(CurrentUser.id(), page, size);
    }

    @GetMapping("/{id}")
    public OrderDetailDto get(@PathVariable Long id) { return orders.myOrder(CurrentUser.id(), id); }

    @PostMapping("/{id}/cancel")
    public OrderDetailDto cancel(@PathVariable Long id, @Valid @RequestBody CancelRequest req) {
        return orders.cancelByCustomer(CurrentUser.id(), id, req.reason());
    }

    @PostMapping("/{id}/reorder")
    public ReorderResponse reorder(@PathVariable Long id) { return orders.reorder(CurrentUser.id(), id); }

    @PostMapping("/{id}/retry-payment")
    public PaymentInit retryPayment(@PathVariable Long id) {
        Long uid = CurrentUser.id();
        return payments.retry(uid, id, users.findById(uid).orElseThrow(() -> ApiException.notFound("User")));
    }

    @GetMapping("/{id}/invoice")
    public InvoiceService.InvoiceDto invoice(@PathVariable Long id) { return invoices.forCustomer(CurrentUser.id(), id); }
}
