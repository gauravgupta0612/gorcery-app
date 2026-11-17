package com.golagrocery.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notifications;
    private final PushSender push;
    private final PushDispatcher dispatcher;

    /** Persists an in-app notification and dispatches an FCM push asynchronously. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyUser(Long userId, String title, String body, String type, String reference) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setTitle(title);
        n.setBody(body);
        n.setType(type);
        n.setReference(reference);
        notifications.save(n);
        dispatcher.pushToUser(userId, title, body, Map.of("type", type, "reference", reference == null ? "" : reference));
    }

    @Transactional
    public int broadcast(String title, String body, String type, String reference) {
        int n = notifications.broadcastToCustomers(title, body, type, reference);
        push.sendToTopic("all-customers", title, body, Map.of("type", type, "reference", reference == null ? "" : reference));
        return n;
    }
}
