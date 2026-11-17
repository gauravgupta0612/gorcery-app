package com.golagrocery.order;

import com.golagrocery.notification.NotificationService;
import com.golagrocery.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotifier {
    private final UserRepository users;
    private final NotificationService notifications;

    public void statusChanged(Order o) {
        try {
            String lang = users.findById(o.getUserId()).map(u -> u.getPreferredLanguage()).orElse("en");
            String[] msg = OrderMessages.of(o.getStatus(), lang, o.getOrderNumber());
            if (msg != null) notifications.notifyUser(o.getUserId(), msg[0], msg[1], "ORDER", String.valueOf(o.getId()));
        } catch (RuntimeException e) {
            // Notifications must never break order processing.
            log.warn("Could not notify user for order {}: {}", o.getId(), e.getMessage());
        }
    }
}
