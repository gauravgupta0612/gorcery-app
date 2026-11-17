package com.golagrocery.payment;

import com.golagrocery.common.Crypto;

/**
 * Local-development gateway (PAYMENTS_MODE=mock). No money moves. Payments are recorded with provider MOCK
 * and the apps label them "TEST PAYMENT". Completion requires an explicit call to /payments/mock/complete.
 */
public class MockGateway implements PaymentGateway {
    @Override public String provider() { return "MOCK"; }
    @Override public String publicKey() { return "mock"; }
    @Override public boolean testMode() { return true; }
    @Override public String createOrder(String receipt, long amountPaise) { return "mock_order_" + Crypto.randomToken(9); }
    @Override public boolean verifyPaymentSignature(String o, String p, String s) { return false; }
    @Override public RefundResult refund(String paymentId, long amountPaise, String note) {
        return new RefundResult("mock_rfnd_" + Crypto.randomToken(9), true);
    }
}
