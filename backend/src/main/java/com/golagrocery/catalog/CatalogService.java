package com.golagrocery.catalog;

import com.golagrocery.catalog.CatalogDtos.*;
import com.golagrocery.common.ApiException;
import com.golagrocery.promo.Banner;
import com.golagrocery.promo.BannerRepository;
import com.golagrocery.cart.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CatalogService {
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final BrandRepository brands;
    private final BannerRepository banners;
    private final WishlistRepository wishlist;
    private final ProductSearchService search;

    @Transactional(readOnly = true)
    public List<CategoryDto> categoryTree(boolean includeInactive) {
        List<Category> all = includeInactive ? categories.findAllByOrderBySortOrderAscNameAsc()
                                             : categories.findByActiveTrueOrderBySortOrderAscNameAsc();
        Map<Long, List<Category>> byParent = new HashMap<>();
        all.forEach(c -> byParent.computeIfAbsent(c.getParentId() == null ? 0L : c.getParentId(), k -> new ArrayList<>()).add(c));
        return byParent.getOrDefault(0L, List.of()).stream().map(c -> toDto(c, byParent)).toList();
    }

    private CategoryDto toDto(Category c, Map<Long, List<Category>> byParent) {
        List<CategoryDto> kids = byParent.getOrDefault(c.getId(), List.of()).stream().map(k -> toDto(k, byParent)).toList();
        return new CategoryDto(c.getId(), c.getName(), c.getNameHi(), c.getSlug(), c.getImageUrl(), c.getParentId(), kids);
    }

    @Transactional(readOnly = true)
    public HomeDto home() {
        Instant now = Instant.now();
        List<BannerDto> bannerDtos = banners.findByActiveTrueOrderBySortOrderAsc().stream()
            .filter(b -> (b.getStartsAt() == null || !b.getStartsAt().isAfter(now)) && (b.getEndsAt() == null || b.getEndsAt().isAfter(now)))
            .map(CatalogService::bannerDto).toList();
        List<CategoryDto> tree = categoryTree(false);
        List<ProductCardDto> featured = products.findFeatured(PageRequest.of(0, 12)).stream().map(ProductCardDto::of).toList();
        var deals = search.search(new ProductSearchService.Criteria(null, null, null, null, null, true, true, "discount", false),
            0, 12, ProductCardDto::of).items();
        List<HomeSection> sections = tree.stream().limit(6).map(c -> new HomeSection(c.name(), c.id(),
            products.findByCategoryTree(c.id(), PageRequest.of(0, 10)).stream().map(ProductCardDto::of).toList()))
            .filter(s -> !s.products().isEmpty()).toList();
        return new HomeDto(bannerDtos, tree, featured, deals, sections);
    }

    public static BannerDto bannerDto(Banner b) {
        return new BannerDto(b.getId(), b.getTitle(), b.getSubtitle(), b.getImageUrl(), b.getLinkType(), b.getLinkValue(), b.getBgColor());
    }

    @Transactional(readOnly = true)
    public ProductDetailDto product(Long id, Long userId) {
        Product p = products.findById(id).filter(Product::isActive).orElseThrow(() -> ApiException.notFound("Product"));
        boolean inWishlist = userId != null && wishlist.existsByUserIdAndProductId(userId, id);
        List<ProductCardDto> similar = products.findByCategoryTree(p.getCategory().getId(), PageRequest.of(0, 11)).stream()
            .filter(o -> !o.getId().equals(id)).limit(10).map(ProductCardDto::of).toList();
        return new ProductDetailDto(p.getId(), p.getName(), p.getNameHi(), p.getSlug(), p.getDescription(),
            p.getBrand() == null ? null : p.getBrand().getName(), p.getCategory().getId(), p.getCategory().getName(),
            p.getCategory().getParentId(), p.getGstRate(), p.getAvgRating(), p.getRatingCount(),
            p.getImages().stream().map(ProductImage::getUrl).toList(),
            p.activeVariants().stream().map(VariantDto::of).toList(), inWishlist, similar);
    }

    @Transactional(readOnly = true)
    public List<BrandDto> brands(Long categoryId) {
        List<Brand> list = categoryId == null ? brands.findAllByOrderByNameAsc() : brands.findForCategory(categoryId);
        return list.stream().filter(Brand::isActive).map(b -> new BrandDto(b.getId(), b.getName())).toList();
    }
}
