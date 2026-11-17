package com.golagrocery.order;

import org.junit.jupiter.api.Test;

import static com.golagrocery.order.OrderStatus.*;
import static org.junit.jupiter.api.Assertions.*;

class OrderStatusTest {
    @Test
    void adminWorkflowFollowsTheDefinedSequence() {
        assertTrue(PLACED.adminCanMoveTo(CONFIRMED));
        assertTrue(CONFIRMED.adminCanMoveTo(PREPARING));
        assertTrue(PREPARING.adminCanMoveTo(PACKED));
        assertFalse(PLACED.adminCanMoveTo(DELIVERED), "cannot skip steps");
        assertFalse(PACKED.adminCanMoveTo(OUT_FOR_DELIVERY), "out-for-delivery is set by the delivery partner");
        assertFalse(DELIVERED.adminCanMoveTo(CANCELLED));
        assertFalse(PENDING_PAYMENT.adminCanMoveTo(CONFIRMED), "unpaid orders cannot be confirmed");
    }

    @Test
    void customerCancellationWindow() {
        assertTrue(PLACED.customerCanCancel());
        assertTrue(PREPARING.customerCanCancel());
        assertFalse(PACKED.customerCanCancel());
        assertFalse(OUT_FOR_DELIVERY.customerCanCancel());
    }

    @Test
    void orderMessagesExistInBothLanguages() {
        for (OrderStatus s : new OrderStatus[]{PLACED, CONFIRMED, PREPARING, PACKED, ASSIGNED, OUT_FOR_DELIVERY, DELIVERED, CANCELLED}) {
            assertNotNull(OrderMessages.of(s, "en", "GG1"));
            assertNotNull(OrderMessages.of(s, "hi", "GG1"));
        }
        assertTrue(OrderMessages.of(DELIVERED, "en", "GG100001")[1].contains("GG100001"));
    }
}
