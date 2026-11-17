package com.golagrocery.payment;

import com.golagrocery.common.Crypto;
import com.golagrocery.config.AppProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RazorpaySignatureTest {
    private final RazorpayGateway gw = new RazorpayGateway(new AppProperties.Razorpay("rzp_test_dummy", "test_secret_value", "whsec"));

    @Test
    void acceptsValidSignatureAndRejectsTampered() {
        String sig = Crypto.hmacSha256Hex("test_secret_value", "order_ABC|pay_XYZ");
        assertTrue(gw.verifyPaymentSignature("order_ABC", "pay_XYZ", sig));
        assertFalse(gw.verifyPaymentSignature("order_ABC", "pay_OTHER", sig));
        assertFalse(gw.verifyPaymentSignature("order_ABC", "pay_XYZ", null));
    }

    @Test
    void webhookSignature() {
        String body = "{\"event\":\"payment.captured\"}";
        assertTrue(gw.verifyWebhookSignature(body, Crypto.hmacSha256Hex("whsec", body)));
        assertFalse(gw.verifyWebhookSignature(body + " ", Crypto.hmacSha256Hex("whsec", body)));
    }

    @Test
    void testModeDetectedFromKey() {
        assertTrue(gw.testMode());
    }

    @Test
    void mockGatewayNeverVerifiesSignatures() {
        assertFalse(new MockGateway().verifyPaymentSignature("a", "b", "c"));
    }
}
