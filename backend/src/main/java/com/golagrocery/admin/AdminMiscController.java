package com.golagrocery.admin;

import com.golagrocery.audit.AuditLog;
import com.golagrocery.audit.AuditLogRepository;
import com.golagrocery.audit.AuditService;
import com.golagrocery.common.ApiException;
import com.golagrocery.common.PageDto;
import com.golagrocery.notification.NotificationService;
import com.golagrocery.payment.Refund;
import com.golagrocery.payment.RefundRepository;
import com.golagrocery.payment.RefundService;
import com.golagrocery.review.Review;
import com.golagrocery.review.ReviewRepository;
import com.golagrocery.security.CurrentUser;
import com.golagrocery.support.SupportController.TicketDto;
import com.golagrocery.support.SupportTicket;
import com.golagrocery.support.SupportTicketRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminMiscController {
    private final RefundRepository refunds;
    private final RefundService refundService;
    private final ReviewRepository reviews;
    private final SupportTicketRepository tickets;
    private final AuditLogRepository auditLogs;
    private final AuditService audit;
    private final NotificationService notifications;
    private final JdbcTemplate jdbc;

    public record RefundRow(Long id, Long orderId, String orderNumber, BigDecimal amount, String reason, String status,
                            String providerRefundId, Instant createdAt, Instant updatedAt) {}
    public record RejectRequest(@Size(max = 200) String note) {}
    public record TicketUpdate(@NotBlank @Pattern(regexp = "OPEN|IN_PROGRESS|RESOLVED|CLOSED") String status, @Size(max = 2000) String reply) {}
    public record Broadcast(@NotBlank @Size(max = 150) String title, @NotBlank @Size(max = 500) String body,
                            @Pattern(regexp = "OFFER|GENERAL") String type) {}

    // ---------------- refunds
    @GetMapping("/refunds")
    @Transactional(readOnly = true)
    public PageDto<RefundRow> refunds(@RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page) {
        var p = status == null || status.isBlank() ? refunds.findAllByOrderByCreatedAtDesc(PageRequest.of(page, 30))
                                                   : refunds.findByStatusOrderByCreatedAtDesc(status, PageRequest.of(page, 30));
        return PageDto.of(p, r -> new RefundRow(r.getId(), r.getOrderId(),
            jdbc.queryForObject("select order_number from orders where id = ?", String.class, r.getOrderId()),
            r.getAmount(), r.getReason(), r.getStatus(), r.getProviderRefundId(), r.getCreatedAt(), r.getUpdatedAt()));
    }

    @PostMapping("/refunds/{id}/process")
    @PreAuthorize("hasRole('ADMIN')")
    public Refund process(@PathVariable Long id) { return refundService.process(id, CurrentUser.id()); }

    @PostMapping("/refunds/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public Refund reject(@PathVariable Long id, @Valid @RequestBody RejectRequest req) { return refundService.reject(id, CurrentUser.id(), req.note()); }

    // ---------------- reviews
    @GetMapping("/reviews")
    @Transactional(readOnly = true)
    public PageDto<Map<String, Object>> reviews(@RequestParam(defaultValue = "0") int page) {
        return PageDto.of(reviews.findAllByOrderByCreatedAtDesc(PageRequest.of(page, 30)), r -> Map.<String, Object>of(
            "id", r.getId(), "productId", r.getProductId(),
            "productName", jdbc.queryForObject("select name from products where id = ?", String.class, r.getProductId()),
            "userId", r.getUserId(), "rating", r.getRating(), "title", r.getTitle() == null ? "" : r.getTitle(),
            "comment", r.getComment() == null ? "" : r.getComment(), "approved", r.isApproved(), "createdAt", r.getCreatedAt()));
    }

    @PatchMapping("/reviews/{id}")
    @Transactional
    public Map<String, Object> moderate(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        Review r = reviews.findById(id).orElseThrow(() -> ApiException.notFound("Review"));
        r.setApproved(Boolean.TRUE.equals(body.get("approved")));
        reviews.saveAndFlush(r);
        reviews.refreshProductRating(r.getProductId());
        audit.log(r.isApproved() ? "REVIEW_APPROVED" : "REVIEW_HIDDEN", "Review", id, null);
        return Map.of("id", id, "approved", r.isApproved());
    }

    // ---------------- support
    @GetMapping("/support/tickets")
    @Transactional(readOnly = true)
    public PageDto<TicketDto> tickets(@RequestParam(defaultValue = "0") int page) {
        return PageDto.of(tickets.findAllByOrderByCreatedAtDesc(PageRequest.of(page, 30)), TicketDto::of);
    }

    @PatchMapping("/support/tickets/{id}")
    @Transactional
    public TicketDto updateTicket(@PathVariable Long id, @Valid @RequestBody TicketUpdate req) {
        SupportTicket t = tickets.findById(id).orElseThrow(() -> ApiException.notFound("Ticket"));
        t.setStatus(req.status());
        if (req.reply() != null && !req.reply().isBlank()) {
            t.setAdminReply(req.reply().trim());
            notifications.notifyUser(t.getUserId(), "Support replied: " + t.getSubject(),
                req.reply().length() > 200 ? req.reply().substring(0, 200) + "…" : req.reply(), "GENERAL", "ticket:" + t.getId());
        }
        return TicketDto.of(tickets.save(t));
    }

    // ---------------- notifications
    @PostMapping("/notifications/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Integer> broadcast(@Valid @RequestBody Broadcast req) {
        int n = notifications.broadcast(req.title(), req.body(), req.type() == null ? "OFFER" : req.type(), null);
        audit.log("BROADCAST_SENT", "Notification", null, req.title());
        return Map.of("recipients", n);
    }

    // ---------------- audit
    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public PageDto<AuditLog> auditLogs(@RequestParam(defaultValue = "0") int page) {
        return PageDto.of(auditLogs.findAllByOrderByCreatedAtDesc(PageRequest.of(page, 50)), a -> a);
    }
}
