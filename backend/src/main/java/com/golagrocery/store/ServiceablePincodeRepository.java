package com.golagrocery.store;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceablePincodeRepository extends JpaRepository<ServiceablePincode, String> {
    List<ServiceablePincode> findAllByOrderByCityAscPincodeAsc();
}
