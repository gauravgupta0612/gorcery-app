package com.golagrocery.promo;

import com.golagrocery.common.Money;
import com.golagrocery.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService {
    private final CouponRepository coupons;
    private final CouponRedemptionRepository redemptions;
    private final OrderRepository orders;

    /** Result of evaluating a coupon. When valid=false, message explains why. */
    public record Evaluation(boolean valid, Coupon coupon, BigDecimal discount, String message) {
        static Evaluation invalid(String msg) { return new Evaluation(false, null, BigDecimal.ZERO, msg); }
    }

    public record CouponDto(String code, String description, String discountType, BigDecimal discountValue,
                            BigDecimal maxDiscount, BigDecimal minOrderAmount, boolean applicable, String reason) {}

    @Transactional(readOnly = true)
    public Evaluation evaluate(String code, Long userId, BigDecimal itemTotal) {
        if (code == null || code.isBlank()) return Evaluation.invalid(null);
        Coupon c = coupons.findByCodeIgnoreCase(code.trim()).orElse(null);
        if (c == null || !c.isActive()) return Evaluation.invalid("Invalid coupon code");
        Instant now = Instant.now();
        if (c.getValidFrom() != null && c.getValidFrom().isAfter(now)) return Evaluation.invalid("This coupon is not active yet");
        if (c.getValidTo() != null && c.getValidTo().isBefore(now)) return Evaluation.invalid("This coupon has expired");
        if (c.getUsageLimit() != null && c.getUsedCount() >= c.getUsageLimit()) return Evaluation.invalid("This coupon has been fully redeemed");
        if (redemptions.countByCouponIdAndUserId(c.getId(), userId) >= c.getPerUserLimit()) return Evaluation.invalid("You have already used this coupon");
        if (c.isFirstOrderOnly() && orders.countSuccessfulByUser(userId) > 0) return Evaluation.invalid("This coupon is valid on your first order only");
        if (itemTotal.compareTo(c.getMinOrderAmount()) < 0) {
            return Evaluation.invalid("Add items worth ₹" + c.getMinOrderAmount().subtract(itemTotal).setScale(0, java.math.RoundingMode.UP) + " more to use this coupon");
        }
        BigDecimal d = "PERCENT".equals(c.getDiscountType())
            ? itemTotal.multiply(c.getDiscountValue()).divide(Money.HUNDRED, 2, java.math.RoundingMode.DOWN)
            : c.getDiscountValue();
        if (c.getMaxDiscount() != null && d.compareTo(c.getMaxDiscount()) > 0) d = c.getMaxDiscount();
        if (d.compareTo(itemTotal) > 0) d = itemTotal;
        return new Evaluation(true, c, Money.round(d), "You saved ₹" + Money.round(d) + " with " + c.getCode());
    }

    @Transactional(readOnly = true)
    public List<CouponDto> available(Long userId, BigDecimal itemTotal) {
        return coupons.findByActiveTrueOrderByMinOrderAmountAsc().stream()
            .filter(c -> c.getValidTo() == null || c.getValidTo().isAfter(Instant.now()))
            .map(c -> {
                Evaluation e = evaluate(c.getCode(), userId, itemTotal);
                return new CouponDto(c.getCode(), c.getDescription(), c.getDiscountType(), c.getDiscountValue(),
                    c.getMaxDiscount(), c.getMinOrderAmount(), e.valid(), e.valid() ? null : e.message());
            }).toList();
    }
}
