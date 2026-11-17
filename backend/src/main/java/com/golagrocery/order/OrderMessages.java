package com.golagrocery.order;

import java.util.Map;

/** Customer-facing order notifications in English and Hindi. */
public final class OrderMessages {
    private OrderMessages() {}

    private static final Map<OrderStatus, String[]> EN = Map.of(
        OrderStatus.PLACED, new String[]{"Order placed", "We have received your order #%s."},
        OrderStatus.CONFIRMED, new String[]{"Order confirmed", "Your order #%s has been confirmed."},
        OrderStatus.PREPARING, new String[]{"Preparing your order", "We are picking fresh items for order #%s."},
        OrderStatus.PACKED, new String[]{"Order packed", "Order #%s is packed and ready."},
        OrderStatus.ASSIGNED, new String[]{"Delivery partner assigned", "A delivery partner will pick up order #%s shortly."},
        OrderStatus.OUT_FOR_DELIVERY, new String[]{"Out for delivery", "Order #%s is on the way. Share the delivery OTP only at your door."},
        OrderStatus.DELIVERED, new String[]{"Delivered", "Order #%s has been delivered. Enjoy your groceries!"},
        OrderStatus.CANCELLED, new String[]{"Order cancelled", "Order #%s has been cancelled."},
        OrderStatus.PAYMENT_FAILED, new String[]{"Payment not completed", "Payment for order #%s was not completed, so the order was cancelled."});

    private static final Map<OrderStatus, String[]> HI = Map.of(
        OrderStatus.PLACED, new String[]{"ऑर्डर प्राप्त हुआ", "हमें आपका ऑर्डर #%s मिल गया है।"},
        OrderStatus.CONFIRMED, new String[]{"ऑर्डर कन्फ़र्म", "आपका ऑर्डर #%s कन्फ़र्म हो गया है।"},
        OrderStatus.PREPARING, new String[]{"ऑर्डर तैयार हो रहा है", "ऑर्डर #%s के लिए ताज़ा सामान चुना जा रहा है।"},
        OrderStatus.PACKED, new String[]{"ऑर्डर पैक हो गया", "ऑर्डर #%s पैक होकर तैयार है।"},
        OrderStatus.ASSIGNED, new String[]{"डिलीवरी पार्टनर तय", "डिलीवरी पार्टनर जल्द ही ऑर्डर #%s लेने आएँगे।"},
        OrderStatus.OUT_FOR_DELIVERY, new String[]{"डिलीवरी के लिए निकला", "ऑर्डर #%s रास्ते में है। OTP केवल दरवाज़े पर ही बताएँ।"},
        OrderStatus.DELIVERED, new String[]{"डिलीवर हो गया", "ऑर्डर #%s डिलीवर हो गया है। धन्यवाद!"},
        OrderStatus.CANCELLED, new String[]{"ऑर्डर रद्द", "ऑर्डर #%s रद्द कर दिया गया है।"},
        OrderStatus.PAYMENT_FAILED, new String[]{"भुगतान पूरा नहीं हुआ", "ऑर्डर #%s का भुगतान पूरा नहीं हुआ, इसलिए ऑर्डर रद्द हो गया।"});

    public static String[] of(OrderStatus status, String lang, String orderNumber) {
        String[] t = ("hi".equals(lang) ? HI : EN).get(status);
        if (t == null) return null;
        return new String[]{t[0], String.format(t[1], orderNumber)};
    }
}
