package com.golagrocery.order;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum OrderStatus {
    PENDING_PAYMENT, PLACED, CONFIRMED, PREPARING, PACKED, ASSIGNED, OUT_FOR_DELIVERY, DELIVERED, CANCELLED, PAYMENT_FAILED;

    /** Transitions allowed from the admin dashboard (assignment and delivery steps have dedicated endpoints). */
    private static final Map<OrderStatus, Set<OrderStatus>> ADMIN_NEXT = Map.of(
        PLACED, EnumSet.of(CONFIRMED, CANCELLED),
        CONFIRMED, EnumSet.of(PREPARING, CANCELLED),
        PREPARING, EnumSet.of(PACKED, CANCELLED),
        PACKED, EnumSet.of(CANCELLED),
        ASSIGNED, EnumSet.of(CANCELLED));

    public boolean adminCanMoveTo(OrderStatus next) {
        return ADMIN_NEXT.getOrDefault(this, Set.of()).contains(next);
    }

    public boolean customerCanCancel() {
        return this == PENDING_PAYMENT || this == PLACED || this == CONFIRMED || this == PREPARING;
    }

    public boolean isActive() {
        return this != DELIVERED && this != CANCELLED && this != PAYMENT_FAILED;
    }
}
