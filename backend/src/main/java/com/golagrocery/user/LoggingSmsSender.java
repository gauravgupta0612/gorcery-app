package com.golagrocery.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Development SMS sender: logs that an SMS would be sent (never logs the OTP itself). */
@Slf4j
@Configuration
public class LoggingSmsSender {
    @Bean
    @ConditionalOnMissingBean(SmsSender.class)
    public SmsSender smsSender() {
        return (phone, message) -> log.info("[SMS-DEV] SMS queued to ******{} ({} chars). Configure a real SmsSender bean for production.",
            phone.substring(Math.max(0, phone.length() - 4)), message.length());
    }
}
