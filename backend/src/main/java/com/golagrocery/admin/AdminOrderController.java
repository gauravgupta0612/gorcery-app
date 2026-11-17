package com.golagrocery.admin;

import com.golagrocery.common.ApiException;
import com.golagrocery.common.PageDto;
import com.golagrocery.delivery.DeliveryService;
import com.golagrocery.order.*;
import com.golagrocery.order.OrderDtos.*;
import com.golagrocery.payment.Refund;
import com.golagrocery.payment.RefundService;
import com.golagrocery.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {
    private final OrderRepository orders;
    private final OrderService orderService;
    private final DeliveryService delivery;
    private final InvoiceService invoices;
    private final RefundService refunds;

    public record StatusUpdate(@NotNull OrderStatus status, @Size(max = 300) String note) {}
    public record AssignRequest(@NotNull Long partnerId) {}
    public record RefundRequest(@Positive BigDecimal amount, @NotBlank @Size(max = 300) String reason) {}

    public record AdminOrderRow(Long id, String orderNumber, String customerName, String customerPhone, String pincode, String status,
                                String paymentMethod, String paymentStatus, BigDecimal grandTotal, int itemCount, Instant placedAt) {}

    @GetMapping
    @Transactional(readOnly = true)
    public PageDto<AdminOrderRow> list(@RequestParam(required = false) List<OrderStatus> status, @RequestParam(required = false) String q,
                                       @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
                                       @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        Set<OrderStatus> statuses = status == null || status.isEmpty() ? EnumSet.allOf(OrderStatus.class) : EnumSet.copyOf(status);
        Instant f = from == null ? Instant.parse("2000-01-01T00:00:00Z") : from.atStartOfDay(com.golagrocery.store.StoreService.IST).toInstant();
        Instant t = to == null ? Instant.parse("2999-01-01T00:00:00Z") : to.plusDays(1).atStartOfDay(com.golagrocery.store.StoreService.IST).toInstant();
        return PageDto.of(orders.adminSearch(statuses, q == null ? "" : q.trim(), f, t, PageRequest.of(page, Math.min(size, 100))),
            o -> new AdminOrderRow(o.getId(), o.getOrderNumber(), o.getShipName(), o.getShipPhone(), o.getShipPincode(), o.getStatus().name(),
                o.getPaymentMethod().name(), o.getPaymentStatus().name(), o.getGrandTotal(),
                o.getItems().stream().mapToInt(OrderItem::getQuantity).sum(), o.getPlacedAt()));
    }

    @GetMapping("/statuses")
    public List<String> statuses() { return Arrays.stream(OrderStatus.values()).map(Enum::name).toList(); }

    @GetMapping("/{id}")
    public OrderDetailDto get(@PathVariable Long id) { return orderService.adminDetail(id); }

    @PatchMapping("/{id}/status")
    public OrderDetailDto status(@PathVariable Long id, @Valid @RequestBody StatusUpdate req) {
        return orderService.adminUpdateStatus(id, req.status(), req.note(), CurrentUser.id());
    }

    @PostMapping("/{id}/assign")
    public OrderDetailDto assign(@PathVariable Long id, @Valid @RequestBody AssignRequest req) {
        return delivery.assign(id, req.partnerId(), CurrentUser.id(), orderService);
    }

    @GetMapping("/{id}/invoice")
    public InvoiceService.InvoiceDto invoice(@PathVariable Long id) { return invoices.forAdmin(id); }

    @PostMapping("/{id}/refunds")
    @PreAuthorize("hasRole('ADMIN')")
    public Refund refund(@PathVariable Long id, @Valid @RequestBody RefundRequest req) {
        if (!orders.existsById(id)) throw ApiException.notFound("Order");
        return refunds.request(id, req.amount(), req.reason(), CurrentUser.id());
    }
}
