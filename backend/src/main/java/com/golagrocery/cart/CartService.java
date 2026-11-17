package com.golagrocery.cart;

import com.golagrocery.catalog.ProductVariant;
import com.golagrocery.catalog.ProductVariantRepository;
import com.golagrocery.common.ApiException;
import com.golagrocery.order.PricingService;
import com.golagrocery.order.PricingService.LineInput;
import com.golagrocery.order.PricingService.Quote;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository carts;
    private final ProductVariantRepository variants;
    private final PricingService pricing;

    @Transactional
    public Cart getOrCreate(Long userId) {
        return carts.findByUserId(userId).orElseGet(() -> {
            Cart c = new Cart();
            c.setUserId(userId);
            return carts.save(c);
        });
    }

    @Transactional
    public Quote view(Long userId, String pincode) {
        Cart c = getOrCreate(userId);
        return pricing.quote(c.getItems().stream().map(i -> new LineInput(i.getVariant(), i.getQuantity())).toList(),
            userId, c.getCouponCode(), pincode);
    }

    /** Sets the quantity of a variant in the cart (0 removes it). Validation errors don't poison a caller's transaction. */
    @Transactional(noRollbackFor = ApiException.class)
    public Quote setQuantity(Long userId, Long variantId, int quantity, String pincode) {
        if (quantity < 0 || quantity > 50) throw ApiException.badRequest("Invalid quantity");
        Cart c = getOrCreate(userId);
        var existing = c.getItems().stream().filter(i -> i.getVariant().getId().equals(variantId)).findFirst();
        if (quantity == 0) {
            existing.ifPresent(i -> c.getItems().remove(i));
        } else {
            ProductVariant v = variants.findById(variantId).orElseThrow(() -> ApiException.notFound("Product"));
            if (!v.isActive() || !v.getProduct().isActive()) throw ApiException.badRequest("This product is no longer available");
            if (v.stock() <= 0) throw ApiException.conflict(v.getProduct().getName() + " is out of stock");
            if (quantity > v.getMaxPerOrder()) throw ApiException.badRequest("You can add up to " + v.getMaxPerOrder() + " of this item");
            if (quantity > v.stock()) throw ApiException.conflict("Only " + v.stock() + " units of " + v.getProduct().getName() + " available");
            if (existing.isPresent()) {
                existing.get().setQuantity(quantity);
            } else {
                if (c.getItems().size() >= 100) throw ApiException.badRequest("Your cart is full");
                CartItem item = new CartItem();
                item.setCart(c);
                item.setVariant(v);
                item.setQuantity(quantity);
                c.getItems().add(item);
            }
        }
        c.setUpdatedAt(Instant.now());
        carts.save(c);
        return view(userId, pincode);
    }

    @Transactional
    public Quote applyCoupon(Long userId, String code, String pincode) {
        Cart c = getOrCreate(userId);
        Quote q = pricing.quote(c.getItems().stream().map(i -> new LineInput(i.getVariant(), i.getQuantity())).toList(), userId, code, pincode);
        if (!q.couponValid()) throw ApiException.badRequest(q.couponMessage() == null ? "Invalid coupon code" : q.couponMessage());
        c.setCouponCode(q.couponCode());
        carts.save(c);
        return q;
    }

    @Transactional
    public Quote removeCoupon(Long userId, String pincode) {
        Cart c = getOrCreate(userId);
        c.setCouponCode(null);
        carts.save(c);
        return view(userId, pincode);
    }

    @Transactional
    public void clear(Long userId) {
        carts.findByUserId(userId).ifPresent(c -> {
            c.getItems().clear();
            c.setCouponCode(null);
            carts.save(c);
        });
    }
}
