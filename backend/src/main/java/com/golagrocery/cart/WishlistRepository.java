package com.golagrocery.cart;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WishlistRepository extends JpaRepository<WishlistItem, WishlistItem.Key> {
    boolean existsByUserIdAndProductId(Long userId, Long productId);
    List<WishlistItem> findByUserIdOrderByAddedAtDesc(Long userId);
    void deleteByUserIdAndProductId(Long userId, Long productId);
}
