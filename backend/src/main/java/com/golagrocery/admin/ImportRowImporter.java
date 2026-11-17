package com.golagrocery.admin;

import com.golagrocery.admin.AdminCatalogDtos.*;
import com.golagrocery.audit.AuditService;
import com.golagrocery.catalog.*;
import com.golagrocery.common.ApiException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Imports one product group in its own transaction so one bad product doesn't abort the whole file. */
@Service
@RequiredArgsConstructor
public class ImportRowImporter {
    private final AdminCatalogService catalog;
    private final ProductVariantRepository variants;
    private final CategoryRepository categories;
    private final ProductRepository products;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public int[] importGroup(List<Map<String, String>> rows, Long actorId) {
        Map<String, String> first = rows.get(0);
        String productName = req(first, "product_name");
        Category cat = resolveCategory(req(first, "category"), first.get("subcategory"));
        // Existing product: matched via any existing SKU in the group, else by slug.
        Product existing = null;
        for (var r : rows) {
            var v = variants.findBySku(req(r, "sku").toUpperCase(Locale.ROOT));
            if (v.isPresent()) { existing = v.get().getProduct(); break; }
        }
        if (existing == null) existing = products.findBySlug(AdminCatalogService.slugify(productName)).orElse(null);

        List<VariantUpsert> vus = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (existing != null) {
            // keep variants not mentioned in the file
            for (ProductVariant v : existing.getVariants()) {
                boolean inFile = rows.stream().anyMatch(r -> req(r, "sku").equalsIgnoreCase(v.getSku()));
                if (!inFile && v.isActive()) {
                    vus.add(new VariantUpsert(v.getId(), v.getSku(), v.getBarcode(), v.getLabel(), v.getQuantityValue(), v.getUnit(),
                        v.getMrp(), v.getSellingPrice(), v.getMaxPerOrder(), v.isDefault(), true, null, null));
                }
            }
        }
        int created = existing == null ? 1 : 0;
        for (int i = 0; i < rows.size(); i++) {
            var r = rows.get(i);
            String sku = req(r, "sku").toUpperCase(Locale.ROOT);
            if (!seen.add(sku)) throw new IllegalArgumentException("duplicate SKU " + sku);
            Long variantId = null;
            if (existing != null) {
                variantId = existing.getVariants().stream().filter(v -> v.getSku().equalsIgnoreCase(sku)).map(ProductVariant::getId).findFirst().orElse(null);
            }
            ProductVariant.Unit unit;
            try {
                unit = ProductVariant.Unit.valueOf(req(r, "unit").toUpperCase(Locale.ROOT).replace("PIECES", "PCS").replace("PC", "PCS").replace("PCSS", "PCS"));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("unit must be one of KG, G, L, ML, PCS");
            }
            vus.add(new VariantUpsert(variantId, sku, opt(r, "barcode"), req(r, "variant_label"), num(r, "quantity_value"), unit,
                num(r, "mrp"), num(r, "selling_price"), 10, vus.isEmpty() && existing == null, !"false".equalsIgnoreCase(opt(r, "active")),
                intOpt(r, "stock"), intOpt(r, "low_stock_threshold")));
        }
        List<String> images = null;
        String img = opt(first, "image_url");
        if (StringUtils.hasText(img)) {
            images = new ArrayList<>(existing == null ? List.of() : existing.getImages().stream().map(ProductImage::getUrl).toList());
            if (!images.contains(img)) images.add(img);
        }
        String gst = opt(first, "gst_rate");
        ProductUpsert pu = new ProductUpsert(productName, existing == null ? null : existing.getNameHi(),
            opt(first, "description") != null ? opt(first, "description") : existing == null ? null : existing.getDescription(),
            null, opt(first, "brand"), cat.getId(), existing == null ? null : existing.getSupplierId(), opt(first, "hsn_code"),
            gst == null ? BigDecimal.ZERO : new BigDecimal(gst), existing != null && existing.isFeatured(), true, vus, images);
        if (existing == null) catalog.create(pu, actorId); else catalog.update(existing.getId(), pu, actorId);
        return new int[]{created, existing == null ? 0 : 1};
    }

    private Category resolveCategory(String parentName, String subName) {
        List<Category> all = categories.findAllByOrderBySortOrderAscNameAsc();
        Category parent = all.stream().filter(c -> c.getParentId() == null && c.getName().equalsIgnoreCase(parentName.trim())).findFirst()
            .orElseGet(() -> catalog.saveCategory(null, new CategoryUpsert(null, parentName.trim(), null, null, 99, true)));
        if (!StringUtils.hasText(subName)) return parent;
        return all.stream().filter(c -> parent.getId().equals(c.getParentId()) && c.getName().equalsIgnoreCase(subName.trim())).findFirst()
            .orElseGet(() -> catalog.saveCategory(null, new CategoryUpsert(parent.getId(), subName.trim(), null, null, 99, true)));
    }

    private static String req(Map<String, String> r, String k) {
        String v = r.get(k);
        if (v == null || v.isBlank()) throw new IllegalArgumentException(k + " is required");
        return v.trim();
    }
    private static String opt(Map<String, String> r, String k) {
        String v = r.get(k);
        return v == null || v.isBlank() ? null : v.trim();
    }
    private static BigDecimal num(Map<String, String> r, String k) {
        try { return AdminCatalogService.parseMoney(req(r, k)); }
        catch (NumberFormatException e) { throw new IllegalArgumentException(k + " must be a number"); }
    }
    private static Integer intOpt(Map<String, String> r, String k) {
        String v = opt(r, k);
        if (v == null) return null;
        try { return Integer.parseInt(v.replace(".0", "")); }
        catch (NumberFormatException e) { throw new IllegalArgumentException(k + " must be a whole number"); }
    }
}
