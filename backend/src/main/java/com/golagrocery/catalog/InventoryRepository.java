package com.golagrocery.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    /** Atomically deducts stock; returns 1 on success, 0 when insufficient stock. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "update inventory set quantity = quantity - :qty, version = version + 1, updated_at = now() where variant_id = :variantId and quantity >= :qty",
           nativeQuery = true)
    int tryDeduct(Long variantId, int qty);

    /** No clearAutomatically: clearing would detach the Order being cancelled (LazyInitializationException on history). */
    @Modifying(flushAutomatically = true)
    @Query(value = "update inventory set quantity = quantity + :qty, version = version + 1, updated_at = now() where variant_id = :variantId",
           nativeQuery = true)
    int restore(Long variantId, int qty);

    @Query(value = "select count(*) from inventory i join product_variants v on v.id = i.variant_id where v.active and i.quantity <= i.low_stock_threshold",
           nativeQuery = true)
    long countLowStock();
}
