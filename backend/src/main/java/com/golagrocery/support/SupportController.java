package com.golagrocery.support;

import com.golagrocery.common.ApiException;
import com.golagrocery.order.OrderRepository;
import com.golagrocery.security.CurrentUser;
import com.golagrocery.store.StoreService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/support")
@RequiredArgsConstructor
public class SupportController {
    private final SupportTicketRepository tickets;
    private final OrderRepository orders;
    private final StoreService store;

    public record CreateTicket(Long orderId, @NotBlank @Size(max = 150) String subject, @NotBlank @Size(max = 2000) String message) {}
    public record TicketDto(Long id, Long orderId, String subject, String message, String status, String adminReply,
                            Instant createdAt, Instant updatedAt) {
        public static TicketDto of(SupportTicket t) {
            return new TicketDto(t.getId(), t.getOrderId(), t.getSubject(), t.getMessage(), t.getStatus(), t.getAdminReply(),
                t.getCreatedAt(), t.getUpdatedAt());
        }
    }

    @GetMapping("/contact")
    public Map<String, String> contact() {
        var s = store.settings();
        return Map.of("phone", s.getSupportPhone() == null ? "" : s.getSupportPhone(),
                      "email", s.getSupportEmail() == null ? "" : s.getSupportEmail());
    }

    @GetMapping("/tickets")
    @Transactional(readOnly = true)
    public List<TicketDto> mine() {
        return tickets.findByUserIdOrderByCreatedAtDesc(CurrentUser.id()).stream().map(TicketDto::of).toList();
    }

    @PostMapping("/tickets")
    @Transactional
    public TicketDto create(@Valid @RequestBody CreateTicket req) {
        Long uid = CurrentUser.id();
        if (req.orderId() != null && orders.findByIdAndUserId(req.orderId(), uid).isEmpty()) throw ApiException.notFound("Order");
        SupportTicket t = new SupportTicket();
        t.setUserId(uid);
        t.setOrderId(req.orderId());
        t.setSubject(req.subject().trim());
        t.setMessage(req.message().trim());
        return TicketDto.of(tickets.save(t));
    }
}
