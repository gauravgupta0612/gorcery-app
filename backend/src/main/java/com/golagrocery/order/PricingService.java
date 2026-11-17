package com.golagrocery.order;

import com.golagrocery.catalog.ProductVariant;
import com.golagrocery.common.Money;
import com.golagrocery.promo.CouponService;
import com.golagrocery.store.ServiceablePincode;
import com.golagrocery.store.StoreService;
import com.golagrocery.store.StoreSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Single source of truth for all amounts. Clients never send prices. */
@Service
@RequiredArgsConstructor
public class PricingService {
    private final StoreService store;
    private final CouponService coupons;

    public record LineInput(ProductVariant variant, int quantity) {}

    public record Line(Long variantId, Long productId, String productName, String variantLabel, String imageUrl,
                       BigDecimal unitMrp, BigDecimal unitPrice, int quantity, BigDecimal lineTotal, BigDecimal lineMrp,
                       BigDecimal gstRate, BigDecimal tax, int availableQty, int maxPerOrder, String problem) {}

    public record Quote(List<Line> lines, BigDecimal itemTotal, BigDecimal mrpTotal, BigDecimal productSavings,
                        String couponCode, boolean couponValid, String couponMessage, BigDecimal couponDiscount,
                        BigDecimal deliveryFee, BigDecimal handlingFee, BigDecimal taxTotal, BigDecimal grandTotal,
                        BigDecimal minOrderAmount, boolean meetsMinimum, BigDecimal freeDeliveryAbove,
                        BigDecimal amountForFreeDelivery, boolean hasProblems, Long couponId) {}

    public Quote quote(List<LineInput> inputs, Long userId, String couponCode, String pincode) {
        StoreSettings s = store.settings();
        List<Line> lines = new ArrayList<>();
        BigDecimal itemTotal = BigDecimal.ZERO, mrpTotal = BigDecimal.ZERO, tax = BigDecimal.ZERO;
        boolean problems = false;
        for (LineInput in : inputs) {
            ProductVariant v = in.variant();
            var p = v.getProduct();
            int stock = v.stock();
            String problem = null;
            if (!p.isActive() || !v.isActive()) problem = "No longer available";
            else if (stock <= 0) problem = "Out of stock";
            else if (in.quantity() > stock) problem = "Only " + stock + " left";
            else if (in.quantity() > v.getMaxPerOrder()) problem = "Max " + v.getMaxPerOrder() + " per order";
            BigDecimal lineTotal = v.getSellingPrice().multiply(BigDecimal.valueOf(in.quantity()));
            BigDecimal lineMrp = v.getMrp().multiply(BigDecimal.valueOf(in.quantity()));
            BigDecimal lineTax = Money.includedTax(lineTotal, p.getGstRate());
            if (problem == null) {
                itemTotal = itemTotal.add(lineTotal);
                mrpTotal = mrpTotal.add(lineMrp);
                tax = tax.add(lineTax);
            } else {
                problems = true;
            }
            lines.add(new Line(v.getId(), p.getId(), p.getName(), v.getLabel(), p.primaryImage(), v.getMrp(), v.getSellingPrice(),
                in.quantity(), Money.round(lineTotal), Money.round(lineMrp), p.getGstRate(), lineTax,
                Math.max(0, Math.min(stock, v.getMaxPerOrder())), v.getMaxPerOrder(), problem));
        }
        CouponService.Evaluation ev = coupons.evaluate(couponCode, userId, itemTotal);
        BigDecimal couponDiscount = ev.valid() ? ev.discount() : BigDecimal.ZERO;
        BigDecimal afterCoupon = itemTotal.subtract(couponDiscount);

        BigDecimal baseFee = s.getDeliveryFee();
        if (pincode != null) {
            baseFee = store.serviceable(pincode).map(ServiceablePincode::getDeliveryFee).filter(f -> f != null).orElse(baseFee);
        }
        boolean free = afterCoupon.compareTo(s.getFreeDeliveryAbove()) >= 0;
        BigDecimal deliveryFee = itemTotal.signum() == 0 || free ? BigDecimal.ZERO : baseFee;
        BigDecimal handling = itemTotal.signum() == 0 ? BigDecimal.ZERO : s.getHandlingFee();
        BigDecimal grand = afterCoupon.add(deliveryFee).add(handling);
        BigDecimal forFree = free ? BigDecimal.ZERO : s.getFreeDeliveryAbove().subtract(afterCoupon);

        return new Quote(lines, Money.round(itemTotal), Money.round(mrpTotal), Money.round(mrpTotal.subtract(itemTotal)),
            ev.valid() ? ev.coupon().getCode() : couponCode, ev.valid(), ev.message(), couponDiscount,
            Money.round(deliveryFee), Money.round(handling), Money.round(tax), Money.round(grand),
            s.getMinOrderAmount(), itemTotal.compareTo(s.getMinOrderAmount()) >= 0, s.getFreeDeliveryAbove(),
            Money.round(forFree), problems, ev.valid() ? ev.coupon().getId() : null);
    }
}
