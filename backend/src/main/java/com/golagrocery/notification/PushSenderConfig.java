package com.golagrocery.notification;

import com.golagrocery.config.AppProperties;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.util.Map;

@Slf4j
@Configuration
public class PushSenderConfig {

    @Bean
    public PushSender pushSender(AppProperties props) {
        String file = props.firebase() == null ? null : props.firebase().credentialsFile();
        if (file == null || file.isBlank()) {
            log.warn("FIREBASE_CREDENTIALS_FILE not set - push notifications are stored in-app only (not sent via FCM)");
            return new PushSender() {
                public boolean sendToToken(String t, String title, String body, Map<String, String> d) { return true; }
                public void sendToTopic(String topic, String title, String body, Map<String, String> d) {}
            };
        }
        try (FileInputStream in = new FileInputStream(file)) {
            FirebaseOptions options = FirebaseOptions.builder().setCredentials(GoogleCredentials.fromStream(in)).build();
            FirebaseApp app = FirebaseApp.getApps().isEmpty() ? FirebaseApp.initializeApp(options) : FirebaseApp.getInstance();
            FirebaseMessaging fcm = FirebaseMessaging.getInstance(app);
            log.info("Firebase Cloud Messaging enabled");
            return new PushSender() {
                public boolean sendToToken(String token, String title, String body, Map<String, String> data) {
                    try {
                        fcm.send(Message.builder().setToken(token)
                            .setNotification(com.google.firebase.messaging.Notification.builder().setTitle(title).setBody(body).build())
                            .putAllData(data == null ? Map.of() : data)
                            .setAndroidConfig(AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH).build())
                            .build());
                        return true;
                    } catch (FirebaseMessagingException e) {
                        if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED
                            || e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                            return false;
                        }
                        log.warn("FCM send failed: {}", e.getMessage());
                        return true;
                    }
                }
                public void sendToTopic(String topic, String title, String body, Map<String, String> data) {
                    try {
                        fcm.send(Message.builder().setTopic(topic)
                            .setNotification(com.google.firebase.messaging.Notification.builder().setTitle(title).setBody(body).build())
                            .putAllData(data == null ? Map.of() : data).build());
                    } catch (FirebaseMessagingException e) {
                        log.warn("FCM topic send failed: {}", e.getMessage());
                    }
                }
            };
        } catch (Exception e) {
            throw new IllegalStateException("Could not initialise Firebase from FIREBASE_CREDENTIALS_FILE", e);
        }
    }
}
