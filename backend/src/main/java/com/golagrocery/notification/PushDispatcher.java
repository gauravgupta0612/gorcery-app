package com.golagrocery.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PushDispatcher {
    private final DeviceTokenRepository tokens;
    private final PushSender push;

    @Async
    @Transactional
    public void pushToUser(Long userId, String title, String body, Map<String, String> data) {
        for (DeviceToken t : tokens.findByUserId(userId)) {
            if (!push.sendToToken(t.getToken(), title, body, data)) {
                tokens.deleteByToken(t.getToken());
            }
        }
    }
}
