package com.golagrocery.cart;

import com.golagrocery.order.PricingService.Quote;
import com.golagrocery.promo.CouponService;
import com.golagrocery.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cart;
    private final CouponService coupons;

    public record SetQty(@NotNull Long variantId, @Min(0) @Max(50) int quantity) {}
    public record ApplyCoupon(@NotBlank @Size(max = 30) String code) {}

    @GetMapping
    public Quote get(@RequestParam(required = false) String pincode) { return cart.view(CurrentUser.id(), pincode); }

    @PutMapping("/items")
    public Quote setItem(@Valid @RequestBody SetQty req, @RequestParam(required = false) String pincode) {
        return cart.setQuantity(CurrentUser.id(), req.variantId(), req.quantity(), pincode);
    }

    @DeleteMapping("/items/{variantId}")
    public Quote remove(@PathVariable Long variantId, @RequestParam(required = false) String pincode) {
        return cart.setQuantity(CurrentUser.id(), variantId, 0, pincode);
    }

    @DeleteMapping
    public Quote clear(@RequestParam(required = false) String pincode) {
        cart.clear(CurrentUser.id());
        return cart.view(CurrentUser.id(), pincode);
    }

    @PostMapping("/coupon")
    public Quote applyCoupon(@Valid @RequestBody ApplyCoupon req, @RequestParam(required = false) String pincode) {
        return cart.applyCoupon(CurrentUser.id(), req.code(), pincode);
    }

    @DeleteMapping("/coupon")
    public Quote removeCoupon(@RequestParam(required = false) String pincode) { return cart.removeCoupon(CurrentUser.id(), pincode); }

    @GetMapping("/coupons")
    public List<CouponService.CouponDto> availableCoupons() {
        Long uid = CurrentUser.id();
        return coupons.available(uid, cart.view(uid, null).itemTotal());
    }
}
