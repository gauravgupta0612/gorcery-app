package com.golagrocery.notification;

import java.util.Map;

public interface PushSender {
    /** @return false when the token is invalid/unregistered and should be removed. */
    boolean sendToToken(String token, String title, String body, Map<String, String> data);
    void sendToTopic(String topic, String title, String body, Map<String, String> data);
}
