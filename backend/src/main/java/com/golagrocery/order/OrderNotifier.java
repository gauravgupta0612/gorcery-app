package com.golagrocery.order;

import com.golagrocery.notification.NotificationService;
import com.golagrocery.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotifier {
    private final UserRepository users;
    private final NotificationService notifications;
    private final InvoiceSender invoiceSender;

    public void statusChanged(Order o) {
        try {
            String lang = users.findById(o.getUserId()).map(u -> u.getPreferredLanguage()).orElse("en");
            String[] msg = OrderMessages.of(o.getStatus(), lang, o.getOrderNumber());
            if (msg != null) notifications.notifyUser(o.getUserId(), msg[0], msg[1], "ORDER", String.valueOf(o.getId()));
        } catch (RuntimeException e) {
            // Notifications must never break order processing.
            log.warn("Could not notify user for order {}: {}", o.getId(), e.getMessage());
        }
        if (o.getStatus() == OrderStatus.PLACED) sendInvoiceAfterCommit(o.getId(), o.getUserId());
    }

    /** COD orders are placed at checkout; online orders when payment is confirmed. Send only once the order is saved. */
    private void sendInvoiceAfterCommit(Long orderId, Long userId) {
        Runnable send = () -> {
            try {
                invoiceSender.send(orderId, userId);
            } catch (RuntimeException e) {
                log.warn("Could not queue invoice for order {}: {}", orderId, e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { send.run(); }
            });
        } else {
            send.run();
        }
    }
}
