package com.golagrocery.catalog;

import com.golagrocery.common.Money;

import java.math.BigDecimal;
import java.util.List;

public final class CatalogDtos {
    private CatalogDtos() {}

    public record VariantDto(Long id, String sku, String label, BigDecimal mrp, BigDecimal price, int discountPercent,
                             boolean inStock, int availableQty, int maxPerOrder, String unit) {
        public static VariantDto of(ProductVariant v) {
            int stock = v.stock();
            return new VariantDto(v.getId(), v.getSku(), v.getLabel(), v.getMrp(), v.getSellingPrice(),
                Money.discountPercent(v.getMrp(), v.getSellingPrice()), stock > 0, Math.min(stock, v.getMaxPerOrder()),
                v.getMaxPerOrder(), v.getUnit().name());
        }
    }

    public record ProductCardDto(Long id, String name, String nameHi, String slug, String brand, String imageUrl,
                                 Long categoryId, BigDecimal avgRating, int ratingCount, List<VariantDto> variants) {
        public static ProductCardDto of(Product p) {
            return new ProductCardDto(p.getId(), p.getName(), p.getNameHi(), p.getSlug(),
                p.getBrand() == null ? null : p.getBrand().getName(), p.primaryImage(), p.getCategory().getId(),
                p.getAvgRating(), p.getRatingCount(), p.activeVariants().stream().map(VariantDto::of).toList());
        }
    }

    public record ProductDetailDto(Long id, String name, String nameHi, String slug, String description, String brand,
                                   Long categoryId, String categoryName, Long parentCategoryId, BigDecimal gstRate,
                                   BigDecimal avgRating, int ratingCount, List<String> images, List<VariantDto> variants,
                                   boolean inWishlist, List<ProductCardDto> similar) {}

    public record CategoryDto(Long id, String name, String nameHi, String slug, String imageUrl, Long parentId,
                              List<CategoryDto> children) {}

    public record BannerDto(Long id, String title, String subtitle, String imageUrl, String linkType, String linkValue, String bgColor) {}

    public record HomeSection(String title, Long categoryId, List<ProductCardDto> products) {}

    public record HomeDto(List<BannerDto> banners, List<CategoryDto> categories, List<ProductCardDto> featured,
                          List<ProductCardDto> deals, List<HomeSection> sections) {}

    public record BrandDto(Long id, String name) {}
}
