package com.golagrocery.payment;

public interface PaymentGateway {
    String provider();
    String publicKey();
    boolean testMode();
    String createOrder(String receipt, long amountPaise);
    boolean verifyPaymentSignature(String providerOrderId, String providerPaymentId, String signature);
    RefundResult refund(String providerPaymentId, long amountPaise, String note);

    record RefundResult(String refundId, boolean processed) {}
}
