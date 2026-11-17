package com.golagrocery.order;

public enum PaymentMethod { UPI, CARD, NETBANKING, COD;
    public boolean isOnline() { return this != COD; }
}
