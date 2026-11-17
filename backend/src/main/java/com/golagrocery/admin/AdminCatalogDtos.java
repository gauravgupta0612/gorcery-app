package com.golagrocery.admin;

import com.golagrocery.catalog.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public final class AdminCatalogDtos {
    private AdminCatalogDtos() {}

    public record VariantUpsert(Long id,
                                @NotBlank @Size(max = 40) @Pattern(regexp = "^[A-Za-z0-9-_]+$", message = "SKU may contain letters, digits, - and _") String sku,
                                @Size(max = 20) @Pattern(regexp = "^\\d{8,14}$|^$", message = "Barcode must be 8-14 digits") String barcode,
                                @NotBlank @Size(max = 40) String label,
                                @NotNull @Positive BigDecimal quantityValue,
                                @NotNull ProductVariant.Unit unit,
                                @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal mrp,
                                @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal sellingPrice,
                                @Min(1) @Max(100) Integer maxPerOrder,
                                @com.fasterxml.jackson.annotation.JsonProperty("isDefault") boolean isDefault,
                                Boolean active,
                                @Min(0) Integer stock,
                                @Min(0) Integer lowStockThreshold) {}

    public record ProductUpsert(@NotBlank @Size(max = 160) String name,
                                @Size(max = 160) String nameHi,
                                @Size(max = 5000) String description,
                                Long brandId,
                                @Size(max = 100) String brandName,
                                @NotNull Long categoryId,
                                Long supplierId,
                                @Size(max = 10) String hsnCode,
                                @NotNull BigDecimal gstRate,
                                @com.fasterxml.jackson.annotation.JsonProperty("isFeatured") boolean isFeatured,
                                boolean active,
                                @NotEmpty @Size(max = 20) List<@Valid VariantUpsert> variants,
                                List<@Size(max = 300) String> imageUrls) {}

    public record AdminVariantDto(Long id, String sku, String barcode, String label, BigDecimal quantityValue, String unit,
                                  BigDecimal mrp, BigDecimal sellingPrice, int discountPercent, int maxPerOrder, @com.fasterxml.jackson.annotation.JsonProperty("isDefault") boolean isDefault,
                                  boolean active, int stock, int lowStockThreshold, String stockStatus) {
        public static AdminVariantDto of(ProductVariant v) {
            Inventory inv = v.getInventory();
            return new AdminVariantDto(v.getId(), v.getSku(), v.getBarcode(), v.getLabel(), v.getQuantityValue(), v.getUnit().name(),
                v.getMrp(), v.getSellingPrice(), com.golagrocery.common.Money.discountPercent(v.getMrp(), v.getSellingPrice()),
                v.getMaxPerOrder(), v.isDefault(), v.isActive(), inv == null ? 0 : inv.getQuantity(),
                inv == null ? 10 : inv.getLowStockThreshold(), inv == null ? "OUT_OF_STOCK" : inv.status());
        }
    }

    public record ImageDto(Long id, String url, int sortOrder) {}

    public record AdminProductDto(Long id, String name, String nameHi, String slug, String description, Long brandId, String brandName,
                                  Long categoryId, String categoryName, Long supplierId, String hsnCode, BigDecimal gstRate,
                                  @com.fasterxml.jackson.annotation.JsonProperty("isFeatured") boolean isFeatured, boolean active, BigDecimal avgRating, int ratingCount,
                                  List<ImageDto> images, List<AdminVariantDto> variants) {
        public static AdminProductDto of(Product p) {
            return new AdminProductDto(p.getId(), p.getName(), p.getNameHi(), p.getSlug(), p.getDescription(),
                p.getBrand() == null ? null : p.getBrand().getId(), p.getBrand() == null ? null : p.getBrand().getName(),
                p.getCategory().getId(), p.getCategory().getName(), p.getSupplierId(), p.getHsnCode(), p.getGstRate(),
                p.isFeatured(), p.isActive(), p.getAvgRating(), p.getRatingCount(),
                p.getImages().stream().map(i -> new ImageDto(i.getId(), i.getUrl(), i.getSortOrder())).toList(),
                p.getVariants().stream().map(AdminVariantDto::of).toList());
        }
    }

    public record CategoryUpsert(Long parentId, @NotBlank @Size(max = 100) String name, @Size(max = 100) String nameHi,
                                 @Size(max = 300) String imageUrl, int sortOrder, boolean active) {}

    public record BrandUpsert(@NotBlank @Size(max = 100) String name, @Size(max = 300) String logoUrl, boolean active) {}

    public record SupplierUpsert(@NotBlank @Size(max = 150) String name, @Size(max = 120) String contactName,
                                 @Pattern(regexp = "^$|^[6-9]\\d{9}$") String phone, @Email String email,
                                 @Pattern(regexp = "^$|^[0-9A-Z]{15}$", message = "GSTIN must be 15 characters") String gstin,
                                 @Size(max = 300) String address, boolean active) {}

    public record StockAdjust(@NotNull Integer delta, @NotBlank @Pattern(regexp = "RESTOCK|ADJUSTMENT|RETURN|DAMAGED") String reason,
                              @Size(max = 60) String reference) {}

    public record StockSettings(@Min(0) int lowStockThreshold) {}

    public record InventoryRow(Long variantId, Long productId, String productName, String brand, String category, String sku,
                               String barcode, String label, int quantity, int lowStockThreshold, String status, boolean active,
                               BigDecimal sellingPrice) {}

    public record ImportResult(int created, int updated, int skipped, List<String> errors) {}
}
