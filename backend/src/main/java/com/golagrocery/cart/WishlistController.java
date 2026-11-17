package com.golagrocery.cart;

import com.golagrocery.catalog.CatalogDtos.ProductCardDto;
import com.golagrocery.catalog.ProductRepository;
import com.golagrocery.common.ApiException;
import com.golagrocery.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
public class WishlistController {
    private final WishlistRepository wishlist;
    private final ProductRepository products;

    @GetMapping
    @Transactional(readOnly = true)
    public List<ProductCardDto> list() {
        var items = wishlist.findByUserIdOrderByAddedAtDesc(CurrentUser.id());
        var byId = products.findAllById(items.stream().map(WishlistItem::getProductId).toList()).stream()
            .collect(Collectors.toMap(p -> p.getId(), Function.identity()));
        return items.stream().map(i -> byId.get(i.getProductId())).filter(p -> p != null && p.isActive())
            .map(ProductCardDto::of).toList();
    }

    @PostMapping("/{productId}")
    @Transactional
    public Map<String, Boolean> add(@PathVariable Long productId) {
        if (!products.existsById(productId)) throw ApiException.notFound("Product");
        Long uid = CurrentUser.id();
        if (!wishlist.existsByUserIdAndProductId(uid, productId)) {
            WishlistItem w = new WishlistItem();
            w.setUserId(uid);
            w.setProductId(productId);
            wishlist.save(w);
        }
        return Map.of("inWishlist", true);
    }

    @DeleteMapping("/{productId}")
    @Transactional
    public ResponseEntity<Map<String, Boolean>> remove(@PathVariable Long productId) {
        wishlist.deleteByUserIdAndProductId(CurrentUser.id(), productId);
        return ResponseEntity.ok(Map.of("inWishlist", false));
    }
}
