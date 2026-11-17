package com.golagrocery.cart;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "wishlist")
@IdClass(WishlistItem.Key.class)
@Getter @Setter
public class WishlistItem {
    @Id private Long userId;
    @Id private Long productId;
    private Instant addedAt = Instant.now();

    public static class Key implements Serializable {
        public Long userId;
        public Long productId;
        public Key() {}
        public Key(Long userId, Long productId) { this.userId = userId; this.productId = productId; }
        @Override public boolean equals(Object o) {
            return o instanceof Key k && Objects.equals(userId, k.userId) && Objects.equals(productId, k.productId);
        }
        @Override public int hashCode() { return Objects.hash(userId, productId); }
    }
}
