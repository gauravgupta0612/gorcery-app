package com.golagrocery.common;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {
    @Test
    void includedGstIsExtractedFromInclusivePrice() {
        // ₹118 inclusive of 18% GST contains ₹18 tax
        assertEquals(new BigDecimal("18.00"), Money.includedTax(new BigDecimal("118.00"), new BigDecimal("18")));
        assertEquals(new BigDecimal("0.00"), Money.includedTax(new BigDecimal("100.00"), BigDecimal.ZERO));
    }

    @Test
    void discountPercentIsFloored() {
        assertEquals(18, Money.discountPercent(new BigDecimal("220"), new BigDecimal("179")));
        assertEquals(0, Money.discountPercent(new BigDecimal("56"), new BigDecimal("56")));
    }

    @Test
    void paiseConversion() {
        assertEquals(29950L, Money.toPaise(new BigDecimal("299.50")));
    }
}
