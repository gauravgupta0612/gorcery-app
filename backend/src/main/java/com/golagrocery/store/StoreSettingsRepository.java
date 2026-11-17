package com.golagrocery.store;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreSettingsRepository extends JpaRepository<StoreSettings, Short> {
    default StoreSettings get() { return findById((short) 1).orElseThrow(); }
}
