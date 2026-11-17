package com.golagrocery.notification;

import com.golagrocery.common.ApiException;
import com.golagrocery.common.PageDto;
import com.golagrocery.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationRepository notifications;
    private final DeviceTokenRepository tokens;

    public record NotificationDto(Long id, String title, String body, String type, String reference, boolean read, Instant createdAt) {
        static NotificationDto of(Notification n) {
            return new NotificationDto(n.getId(), n.getTitle(), n.getBody(), n.getType(), n.getReference(), n.isRead(), n.getCreatedAt());
        }
    }
    public record RegisterDevice(@NotBlank @Size(max = 300) String token) {}

    @GetMapping("/notifications")
    @Transactional(readOnly = true)
    public PageDto<NotificationDto> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "30") int size) {
        return PageDto.of(notifications.findByUserIdOrderByCreatedAtDesc(CurrentUser.id(), PageRequest.of(page, Math.min(size, 100))),
            NotificationDto::of);
    }

    @GetMapping("/notifications/unread-count")
    public Map<String, Long> unread() { return Map.of("count", notifications.countByUserIdAndIsReadFalse(CurrentUser.id())); }

    @PostMapping("/notifications/{id}/read")
    @Transactional
    public void markRead(@PathVariable Long id) {
        Notification n = notifications.findByIdAndUserId(id, CurrentUser.id()).orElseThrow(() -> ApiException.notFound("Notification"));
        n.setRead(true);
        notifications.save(n);
    }

    @PostMapping("/notifications/read-all")
    @Transactional
    public Map<String, Integer> markAll() { return Map.of("updated", notifications.markAllRead(CurrentUser.id())); }

    @PostMapping("/devices")
    @Transactional
    public void registerDevice(@Valid @RequestBody RegisterDevice req) {
        DeviceToken t = tokens.findByToken(req.token()).orElseGet(DeviceToken::new);
        t.setToken(req.token());
        t.setUserId(CurrentUser.id());
        t.setUpdatedAt(Instant.now());
        tokens.save(t);
    }
}
