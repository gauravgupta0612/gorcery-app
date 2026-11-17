package com.golagrocery.store;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StoreService {
    public static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private final StoreSettingsRepository settings;
    private final ServiceablePincodeRepository pincodes;

    @Transactional(readOnly = true)
    public StoreSettings settings() { return settings.get(); }

    public boolean isOpenNow(StoreSettings s) {
        if (!s.isStoreOpen()) return false;
        LocalTime now = LocalTime.now(IST);
        if (s.getOpeningTime().isBefore(s.getClosingTime())) {
            return !now.isBefore(s.getOpeningTime()) && now.isBefore(s.getClosingTime());
        }
        return !now.isBefore(s.getOpeningTime()) || now.isBefore(s.getClosingTime()); // overnight window
    }

    @Transactional(readOnly = true)
    public Optional<ServiceablePincode> serviceable(String pincode) {
        return pincodes.findById(pincode).filter(ServiceablePincode::isActive);
    }
}
