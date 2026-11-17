package com.golagrocery.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {
    private Money() {}
    public static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public static BigDecimal round(BigDecimal v) { return v.setScale(2, RoundingMode.HALF_UP); }

    /** GST component contained in a tax-inclusive amount: amount * rate / (100 + rate). */
    public static BigDecimal includedTax(BigDecimal inclusiveAmount, BigDecimal ratePercent) {
        if (ratePercent == null || ratePercent.signum() == 0) return BigDecimal.ZERO.setScale(2);
        return inclusiveAmount.multiply(ratePercent).divide(HUNDRED.add(ratePercent), 2, RoundingMode.HALF_UP);
    }

    public static int discountPercent(BigDecimal mrp, BigDecimal price) {
        if (mrp == null || mrp.signum() == 0 || price.compareTo(mrp) >= 0) return 0;
        return mrp.subtract(price).multiply(HUNDRED).divide(mrp, 0, RoundingMode.DOWN).intValue();
    }

    public static long toPaise(BigDecimal rupees) {
        return rupees.multiply(HUNDRED).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
