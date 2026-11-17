package com.golagrocery.admin;

import com.golagrocery.audit.AuditService;
import com.golagrocery.common.ApiException;
import com.golagrocery.promo.*;
import com.golagrocery.store.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminStoreController {
    private final StoreSettingsRepository settings;
    private final ServiceablePincodeRepository pincodes;
    private final CouponRepository coupons;
    private final BannerRepository banners;
    private final AuditService audit;

    public record SettingsUpdate(@NotBlank @Size(max = 100) String storeName, @NotBlank @Size(max = 150) String tagline,
                                 @Size(max = 15) String supportPhone, @Email String supportEmail,
                                 @Pattern(regexp = "^$|^[0-9A-Z]{15}$") String gstin, @Size(max = 300) String storeAddress,
                                 @NotNull @PositiveOrZero BigDecimal deliveryFee, @NotNull @PositiveOrZero BigDecimal freeDeliveryAbove,
                                 @NotNull @PositiveOrZero BigDecimal handlingFee, @NotNull @PositiveOrZero BigDecimal minOrderAmount,
                                 boolean codEnabled, @NotNull @PositiveOrZero BigDecimal codMaxAmount, boolean storeOpen,
                                 @NotNull LocalTime openingTime, @NotNull LocalTime closingTime) {}

    public record PincodeUpsert(@NotBlank @Pattern(regexp = "^[1-9]\\d{5}$") String pincode, @NotBlank @Size(max = 80) String city,
                                @Size(max = 120) String area, @PositiveOrZero BigDecimal deliveryFee, @Min(5) @Max(240) int etaMinutes,
                                boolean active) {}

    public record CouponUpsert(@NotBlank @Pattern(regexp = "^[A-Za-z0-9]{3,30}$", message = "3-30 letters/digits") String code,
                               @Size(max = 200) String description, @NotBlank @Pattern(regexp = "PERCENT|FLAT") String discountType,
                               @NotNull @Positive BigDecimal discountValue, @Positive BigDecimal maxDiscount,
                               @NotNull @PositiveOrZero BigDecimal minOrderAmount, @Positive Integer usageLimit,
                               @Min(1) int perUserLimit, Instant validFrom, Instant validTo, boolean firstOrderOnly, boolean active) {}

    public record BannerUpsert(@NotBlank @Size(max = 120) String title, @Size(max = 200) String subtitle,
                               @NotBlank @Size(max = 300) String imageUrl, @NotBlank @Pattern(regexp = "NONE|CATEGORY|PRODUCT|COUPON") String linkType,
                               @Size(max = 60) String linkValue, @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String bgColor, int sortOrder,
                               boolean active, Instant startsAt, Instant endsAt) {}

    // ---------------- settings
    @GetMapping("/settings")
    public StoreSettings getSettings() { return settings.get(); }

    @PutMapping("/settings")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public StoreSettings updateSettings(@Valid @RequestBody SettingsUpdate r) {
        StoreSettings s = settings.get();
        s.setStoreName(r.storeName());
        s.setTagline(r.tagline());
        s.setSupportPhone(r.supportPhone());
        s.setSupportEmail(r.supportEmail());
        s.setGstin(r.gstin() == null || r.gstin().isBlank() ? null : r.gstin());
        s.setStoreAddress(r.storeAddress());
        s.setDeliveryFee(r.deliveryFee());
        s.setFreeDeliveryAbove(r.freeDeliveryAbove());
        s.setHandlingFee(r.handlingFee());
        s.setMinOrderAmount(r.minOrderAmount());
        s.setCodEnabled(r.codEnabled());
        s.setCodMaxAmount(r.codMaxAmount());
        s.setStoreOpen(r.storeOpen());
        s.setOpeningTime(r.openingTime());
        s.setClosingTime(r.closingTime());
        s.setUpdatedAt(Instant.now());
        audit.log("SETTINGS_UPDATED", "StoreSettings", 1, null);
        return settings.save(s);
    }

    // ---------------- pincodes
    @GetMapping("/pincodes")
    public List<ServiceablePincode> pincodes() { return pincodes.findAllByOrderByCityAscPincodeAsc(); }

    @PutMapping("/pincodes/{pincode}")
    @Transactional
    public ServiceablePincode upsertPincode(@PathVariable String pincode, @Valid @RequestBody PincodeUpsert r) {
        if (!pincode.equals(r.pincode())) throw ApiException.badRequest("Pincode mismatch");
        ServiceablePincode p = pincodes.findById(pincode).orElseGet(ServiceablePincode::new);
        p.setPincode(r.pincode());
        p.setCity(r.city());
        p.setArea(r.area());
        p.setDeliveryFee(r.deliveryFee());
        p.setEtaMinutes(r.etaMinutes());
        p.setActive(r.active());
        audit.log("PINCODE_SAVED", "Pincode", pincode, null);
        return pincodes.save(p);
    }

    @DeleteMapping("/pincodes/{pincode}")
    @Transactional
    public ResponseEntity<Void> deletePincode(@PathVariable String pincode) {
        pincodes.deleteById(pincode);
        audit.log("PINCODE_DELETED", "Pincode", pincode, null);
        return ResponseEntity.noContent().build();
    }

    // ---------------- coupons
    @GetMapping("/coupons")
    public List<Coupon> coupons() { return coupons.findAllByOrderByCreatedAtDesc(); }

    @PostMapping("/coupons")
    @Transactional
    public Coupon createCoupon(@Valid @RequestBody CouponUpsert r) {
        if (coupons.findByCodeIgnoreCase(r.code()).isPresent()) throw ApiException.conflict("Coupon code already exists");
        Coupon c = applyCoupon(new Coupon(), r);
        audit.log("COUPON_CREATED", "Coupon", c.getCode(), null);
        return coupons.save(c);
    }

    @PutMapping("/coupons/{id}")
    @Transactional
    public Coupon updateCoupon(@PathVariable Long id, @Valid @RequestBody CouponUpsert r) {
        Coupon c = coupons.findById(id).orElseThrow(() -> ApiException.notFound("Coupon"));
        coupons.findByCodeIgnoreCase(r.code()).filter(o -> !o.getId().equals(id)).ifPresent(o -> { throw ApiException.conflict("Coupon code already exists"); });
        audit.log("COUPON_UPDATED", "Coupon", id, null);
        return coupons.save(applyCoupon(c, r));
    }

    @DeleteMapping("/coupons/{id}")
    @Transactional
    public ResponseEntity<Void> deleteCoupon(@PathVariable Long id) {
        Coupon c = coupons.findById(id).orElseThrow(() -> ApiException.notFound("Coupon"));
        if (c.getUsedCount() > 0) {
            c.setActive(false);   // keep history
            coupons.save(c);
        } else {
            coupons.delete(c);
        }
        audit.log("COUPON_DELETED", "Coupon", id, null);
        return ResponseEntity.noContent().build();
    }

    private Coupon applyCoupon(Coupon c, CouponUpsert r) {
        if ("PERCENT".equals(r.discountType()) && r.discountValue().compareTo(BigDecimal.valueOf(90)) > 0) {
            throw ApiException.badRequest("Percentage discount cannot exceed 90%");
        }
        if (r.validFrom() != null && r.validTo() != null && !r.validTo().isAfter(r.validFrom())) {
            throw ApiException.badRequest("End date must be after start date");
        }
        c.setCode(r.code().toUpperCase(Locale.ROOT));
        c.setDescription(r.description());
        c.setDiscountType(r.discountType());
        c.setDiscountValue(r.discountValue());
        c.setMaxDiscount(r.maxDiscount());
        c.setMinOrderAmount(r.minOrderAmount());
        c.setUsageLimit(r.usageLimit());
        c.setPerUserLimit(r.perUserLimit());
        c.setValidFrom(r.validFrom() == null ? Instant.now() : r.validFrom());
        c.setValidTo(r.validTo());
        c.setFirstOrderOnly(r.firstOrderOnly());
        c.setActive(r.active());
        return c;
    }

    // ---------------- banners
    @GetMapping("/banners")
    public List<Banner> banners() { return banners.findAllByOrderBySortOrderAsc(); }

    @PostMapping("/banners")
    @Transactional
    public Banner createBanner(@Valid @RequestBody BannerUpsert r) { return banners.save(applyBanner(new Banner(), r)); }

    @PutMapping("/banners/{id}")
    @Transactional
    public Banner updateBanner(@PathVariable Long id, @Valid @RequestBody BannerUpsert r) {
        return banners.save(applyBanner(banners.findById(id).orElseThrow(() -> ApiException.notFound("Banner")), r));
    }

    @DeleteMapping("/banners/{id}")
    @Transactional
    public ResponseEntity<Void> deleteBanner(@PathVariable Long id) {
        banners.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private Banner applyBanner(Banner b, BannerUpsert r) {
        b.setTitle(r.title());
        b.setSubtitle(r.subtitle());
        b.setImageUrl(r.imageUrl());
        b.setLinkType(r.linkType());
        b.setLinkValue(r.linkValue());
        b.setBgColor(r.bgColor() == null ? "#0C831F" : r.bgColor());
        b.setSortOrder(r.sortOrder());
        b.setActive(r.active());
        b.setStartsAt(r.startsAt());
        b.setEndsAt(r.endsAt());
        audit.log("BANNER_SAVED", "Banner", b.getId(), r.title());
        return b;
    }
}
