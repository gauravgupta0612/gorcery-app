package com.golagrocery.store;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/store")
@RequiredArgsConstructor
public class StoreController {
    private final StoreService store;

    public record PublicSettings(String storeName, String tagline, String supportPhone, String supportEmail,
                                 BigDecimal deliveryFee, BigDecimal freeDeliveryAbove, BigDecimal handlingFee,
                                 BigDecimal minOrderAmount, boolean codEnabled, BigDecimal codMaxAmount, boolean openNow,
                                 String openingTime, String closingTime) {}

    public record PincodeCheck(String pincode, boolean serviceable, String city, String area, Integer etaMinutes, String message) {}

    @GetMapping("/settings")
    public PublicSettings settings() {
        StoreSettings s = store.settings();
        return new PublicSettings(s.getStoreName(), s.getTagline(), s.getSupportPhone(), s.getSupportEmail(), s.getDeliveryFee(),
            s.getFreeDeliveryAbove(), s.getHandlingFee(), s.getMinOrderAmount(), s.isCodEnabled(), s.getCodMaxAmount(),
            store.isOpenNow(s), s.getOpeningTime().toString(), s.getClosingTime().toString());
    }

    @GetMapping("/pincodes/{pincode}")
    public PincodeCheck check(@PathVariable String pincode) {
        if (!pincode.matches("^[1-9]\\d{5}$")) {
            return new PincodeCheck(pincode, false, null, null, null, "Please enter a valid 6-digit pincode");
        }
        return store.serviceable(pincode)
            .map(p -> new PincodeCheck(pincode, true, p.getCity(), p.getArea(), p.getEtaMinutes(),
                "Delivery in " + p.getEtaMinutes() + " minutes"))
            .orElse(new PincodeCheck(pincode, false, null, null, null, "Sorry, we don't deliver to this pincode yet"));
    }
}
