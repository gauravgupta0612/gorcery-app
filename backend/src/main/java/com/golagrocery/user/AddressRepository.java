package com.golagrocery.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserIdOrderByIsDefaultDescIdDesc(Long userId);
    Optional<Address> findByIdAndUserId(Long id, Long userId);
    long countByUserId(Long userId);

    @Modifying
    @Query("update Address a set a.isDefault = false where a.userId = :userId")
    void clearDefault(Long userId);
}
