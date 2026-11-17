package com.golagrocery.user;

/** Sends transactional SMS. Plug in MSG91 / Twilio / AWS SNS by providing another bean. */
public interface SmsSender {
    void send(String phone, String message);
}
