package com.golagrocery.admin;

import com.golagrocery.admin.AdminCatalogDtos.*;
import com.golagrocery.audit.AuditService;
import com.golagrocery.catalog.*;
import com.golagrocery.common.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminCatalogService {
    private static final Set<Integer> GST = Set.of(0, 5, 12, 18, 28);
    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final CategoryRepository categories;
    private final BrandRepository brands;
    private final SupplierRepository suppliers;
    private final InventoryRepository inventory;
    private final InventoryMovementRepository movements;
    private final AuditService audit;
    private final JdbcTemplate jdbc;

    public static String slugify(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = n.toLowerCase(Locale.ROOT).replace("&", "and").replace("'", "").replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return slug.isEmpty() ? "item" : slug;
    }

    private String uniqueProductSlug(String name, Long excludeId) {
        String base = slugify(name), slug = base;
        int i = 2;
        while (true) {
            Optional<Product> ex = products.findBySlug(slug);
            if (ex.isEmpty() || ex.get().getId().equals(excludeId)) return slug;
            slug = base + "-" + i++;
        }
    }

    @Transactional(readOnly = true)
    public AdminProductDto get(Long id) {
        return AdminProductDto.of(products.findById(id).orElseThrow(() -> ApiException.notFound("Product")));
    }

    @Transactional
    public AdminProductDto create(ProductUpsert req, Long actorId) {
        Product p = new Product();
        apply(p, req, actorId);
        audit.log("PRODUCT_CREATED", "Product", p.getId(), p.getName());
        return AdminProductDto.of(p);
    }

    @Transactional
    public AdminProductDto update(Long id, ProductUpsert req, Long actorId) {
        Product p = products.findById(id).orElseThrow(() -> ApiException.notFound("Product"));
        apply(p, req, actorId);
        audit.log("PRODUCT_UPDATED", "Product", p.getId(), p.getName());
        return AdminProductDto.of(p);
    }

    private void apply(Product p, ProductUpsert req, Long actorId) {
        if (!GST.contains(req.gstRate().intValue()) || req.gstRate().stripTrailingZeros().scale() > 0) {
            throw ApiException.badRequest("GST rate must be one of 0, 5, 12, 18, 28");
        }
        Category cat = categories.findById(req.categoryId()).orElseThrow(() -> ApiException.notFound("Category"));
        long defaults = req.variants().stream().filter(VariantUpsert::isDefault).count();
        if (defaults > 1) throw ApiException.badRequest("Only one variant can be the default");
        Set<String> skus = new HashSet<>();
        for (VariantUpsert v : req.variants()) {
            if (v.sellingPrice().compareTo(v.mrp()) > 0) throw ApiException.badRequest("Selling price cannot exceed MRP for " + v.label());
            if (!skus.add(v.sku().toUpperCase(Locale.ROOT))) throw ApiException.badRequest("Duplicate SKU " + v.sku());
        }
        p.setName(req.name().trim());
        p.setNameHi(req.nameHi());
        p.setSlug(uniqueProductSlug(req.name(), p.getId()));
        p.setDescription(req.description());
        p.setCategory(cat);
        p.setBrand(resolveBrand(req.brandId(), req.brandName()));
        if (req.supplierId() != null && !suppliers.existsById(req.supplierId())) throw ApiException.notFound("Supplier");
        p.setSupplierId(req.supplierId());
        p.setHsnCode(req.hsnCode());
        p.setGstRate(req.gstRate());
        p.setFeatured(req.isFeatured());
        p.setActive(req.active());
        if (p.getId() == null) products.saveAndFlush(p);

        Map<Long, ProductVariant> existing = new HashMap<>();
        p.getVariants().forEach(v -> existing.put(v.getId(), v));
        Set<Long> keep = new HashSet<>();
        boolean anyDefault = defaults == 1;
        int idx = 0;
        for (VariantUpsert vu : req.variants()) {
            ProductVariant v = vu.id() == null ? null : existing.get(vu.id());
            if (vu.id() != null && v == null) throw ApiException.badRequest("Variant " + vu.id() + " does not belong to this product");
            boolean isNew = v == null;
            String sku = vu.sku().trim().toUpperCase(Locale.ROOT);
            variants.findBySku(sku).filter(o -> isNew || !o.getId().equals(vu.id()))
                .ifPresent(o -> { throw ApiException.conflict("SKU " + sku + " is already used by another product"); });
            String barcode = StringUtils.hasText(vu.barcode()) ? vu.barcode().trim() : null;
            if (isNew) {
                v = new ProductVariant();
                v.setProduct(p);
                p.getVariants().add(v);
            } else if (barcode != null && !barcode.equals(v.getBarcode()) && variants.existsByBarcode(barcode)) {
                throw ApiException.conflict("Barcode " + barcode + " is already in use");
            }
            if (isNew && barcode != null && variants.existsByBarcode(barcode)) throw ApiException.conflict("Barcode " + barcode + " is already in use");
            v.setSku(sku);
            v.setBarcode(barcode);
            v.setLabel(vu.label().trim());
            v.setQuantityValue(vu.quantityValue());
            v.setUnit(vu.unit());
            v.setMrp(vu.mrp());
            v.setSellingPrice(vu.sellingPrice());
            v.setMaxPerOrder(vu.maxPerOrder() == null ? 10 : vu.maxPerOrder());
            v.setDefault(anyDefault ? vu.isDefault() : idx == 0);
            v.setActive(vu.active() == null || vu.active());
            variants.saveAndFlush(v);
            keep.add(v.getId());

            Inventory inv = inventory.findById(v.getId()).orElse(null);
            if (inv == null) {
                inv = new Inventory();
                inv.setVariantId(v.getId());
                inv.setQuantity(0);
            }
            if (vu.lowStockThreshold() != null) inv.setLowStockThreshold(vu.lowStockThreshold());
            int target = vu.stock() == null ? inv.getQuantity() : vu.stock();
            int delta = target - inv.getQuantity();
            inv.setQuantity(target);
            inventory.save(inv);
            if (delta != 0) movement(v.getId(), delta, isNew ? "RESTOCK" : "ADJUSTMENT", "product-form", actorId);
            idx++;
        }
        // Variants removed from the form are deactivated (they may be referenced by past orders).
        p.getVariants().stream().filter(v -> v.getId() != null && !keep.contains(v.getId())).forEach(v -> {
            v.setActive(false);
            v.setDefault(false);
        });

        if (req.imageUrls() != null) {
            p.getImages().clear();
            int i = 0;
            for (String url : req.imageUrls()) {
                if (!StringUtils.hasText(url)) continue;
                ProductImage img = new ProductImage();
                img.setProduct(p);
                img.setUrl(url.trim());
                img.setSortOrder(i++);
                p.getImages().add(img);
            }
        }
        products.saveAndFlush(p);
    }

    Brand resolveBrand(Long brandId, String brandName) {
        if (brandId != null) return brands.findById(brandId).orElseThrow(() -> ApiException.notFound("Brand"));
        if (!StringUtils.hasText(brandName)) return null;
        return brands.findByNameIgnoreCase(brandName.trim()).orElseGet(() -> {
            Brand b = new Brand();
            b.setName(brandName.trim());
            return brands.save(b);
        });
    }

    void movement(Long variantId, int delta, String reason, String ref, Long actorId) {
        InventoryMovement m = new InventoryMovement();
        m.setVariantId(variantId);
        m.setDelta(delta);
        m.setReason(reason);
        m.setReference(ref);
        m.setCreatedBy(actorId);
        movements.save(m);
    }

    @Transactional
    public void delete(Long id) {
        Product p = products.findById(id).orElseThrow(() -> ApiException.notFound("Product"));
        Integer used = jdbc.queryForObject("select count(*) from order_items where product_id = ?", Integer.class, id);
        if (used != null && used > 0) {
            p.setActive(false);
            products.save(p);
            audit.log("PRODUCT_DEACTIVATED", "Product", id, "Has order history - deactivated instead of deleted");
        } else {
            products.delete(p);
            audit.log("PRODUCT_DELETED", "Product", id, p.getName());
        }
    }

    @Transactional
    public AdminProductDto setActive(Long id, boolean active) {
        Product p = products.findById(id).orElseThrow(() -> ApiException.notFound("Product"));
        p.setActive(active);
        audit.log(active ? "PRODUCT_ACTIVATED" : "PRODUCT_DEACTIVATED", "Product", id, null);
        return AdminProductDto.of(products.save(p));
    }

    @Transactional
    public AdminProductDto addImage(Long id, String url) {
        Product p = products.findById(id).orElseThrow(() -> ApiException.notFound("Product"));
        if (p.getImages().size() >= 10) throw ApiException.badRequest("A product can have up to 10 images");
        ProductImage img = new ProductImage();
        img.setProduct(p);
        img.setUrl(url);
        img.setSortOrder(p.getImages().stream().mapToInt(ProductImage::getSortOrder).max().orElse(-1) + 1);
        p.getImages().add(img);
        return AdminProductDto.of(products.saveAndFlush(p));
    }

    @Transactional
    public AdminProductDto removeImage(Long id, Long imageId) {
        Product p = products.findById(id).orElseThrow(() -> ApiException.notFound("Product"));
        if (!p.getImages().removeIf(i -> i.getId().equals(imageId))) throw ApiException.notFound("Image");
        return AdminProductDto.of(products.saveAndFlush(p));
    }

    // ------------------------------------------------------------ categories
    @Transactional
    public Category saveCategory(Long id, CategoryUpsert req) {
        Category c = id == null ? new Category() : categories.findById(id).orElseThrow(() -> ApiException.notFound("Category"));
        if (req.parentId() != null) {
            if (req.parentId().equals(id)) throw ApiException.badRequest("A category cannot be its own parent");
            Category parent = categories.findById(req.parentId()).orElseThrow(() -> ApiException.notFound("Parent category"));
            if (id != null && id.equals(parent.getParentId())) throw ApiException.badRequest("Circular category hierarchy");
        }
        c.setParentId(req.parentId());
        c.setName(req.name().trim());
        c.setNameHi(req.nameHi());
        c.setImageUrl(req.imageUrl());
        c.setSortOrder(req.sortOrder());
        c.setActive(req.active());
        if (c.getSlug() == null) {
            String base = slugify(req.name()), slug = base;
            int i = 2;
            while (categories.existsBySlug(slug)) slug = base + "-" + i++;
            c.setSlug(slug);
        }
        Category saved = categories.save(c);
        audit.log(id == null ? "CATEGORY_CREATED" : "CATEGORY_UPDATED", "Category", saved.getId(), saved.getName());
        return saved;
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category c = categories.findById(id).orElseThrow(() -> ApiException.notFound("Category"));
        if (categories.countByParentId(id) > 0) throw ApiException.conflict("Delete or move its subcategories first");
        if (products.countByCategoryId(id) > 0) throw ApiException.conflict("Move or delete the products in this category first");
        categories.delete(c);
        audit.log("CATEGORY_DELETED", "Category", id, c.getName());
    }

    // ------------------------------------------------------------ inventory
    @Transactional
    public Inventory adjustStock(Long variantId, StockAdjust req, Long actorId) {
        if (!variants.existsById(variantId)) throw ApiException.notFound("Variant");
        if (req.delta() == 0) throw ApiException.badRequest("Quantity change cannot be zero");
        if (req.delta() > 0) {
            inventory.restore(variantId, req.delta());
        } else if (inventory.tryDeduct(variantId, -req.delta()) == 0) {
            throw ApiException.badRequest("Stock cannot go below zero");
        }
        movement(variantId, req.delta(), req.reason(), req.reference(), actorId);
        audit.log("STOCK_" + req.reason(), "Variant", variantId, "delta=" + req.delta());
        return inventory.findById(variantId).orElseThrow();
    }

    @Transactional
    public Inventory setThreshold(Long variantId, int threshold) {
        Inventory inv = inventory.findById(variantId).orElseThrow(() -> ApiException.notFound("Inventory"));
        inv.setLowStockThreshold(threshold);
        return inventory.save(inv);
    }

    @Transactional(readOnly = true)
    public List<InventoryRow> inventory(String q, boolean lowOnly) {
        StringBuilder sql = new StringBuilder("""
            select v.id, p.id, p.name, b.name, c.name, v.sku, v.barcode, v.label, i.quantity, i.low_stock_threshold, v.active, v.selling_price
            from product_variants v join products p on p.id = v.product_id join inventory i on i.variant_id = v.id
            left join brands b on b.id = p.brand_id join categories c on c.id = p.category_id where 1=1
            """);
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(q)) {
            sql.append(" and (p.name ilike ? or v.sku ilike ? or coalesce(v.barcode,'') = ?)");
            String like = "%" + q.trim().replace("%", "\\%").replace("_", "\\_") + "%";
            args.add(like); args.add(like); args.add(q.trim());
        }
        if (lowOnly) sql.append(" and v.active and i.quantity <= i.low_stock_threshold");
        sql.append(" order by i.quantity asc, p.name limit 500");
        return jdbc.query(sql.toString(), (rs, n) -> {
            int qty = rs.getInt(9), th = rs.getInt(10);
            return new InventoryRow(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6),
                rs.getString(7), rs.getString(8), qty, th, qty <= 0 ? "OUT_OF_STOCK" : qty <= th ? "LOW_STOCK" : "IN_STOCK",
                rs.getBoolean(11), rs.getBigDecimal(12));
        }, args.toArray());
    }

    public static BigDecimal parseMoney(String s) { return new BigDecimal(s.trim().replace(",", "").replace("₹", "")); }
}
