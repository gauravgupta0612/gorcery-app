package com.golagrocery.admin;

import com.golagrocery.admin.AdminCatalogDtos.*;
import com.golagrocery.catalog.*;
import com.golagrocery.catalog.CatalogDtos.CategoryDto;
import com.golagrocery.common.ApiException;
import com.golagrocery.common.PageDto;
import com.golagrocery.files.FileStorageService;
import com.golagrocery.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminCatalogController {
    private final AdminCatalogService service;
    private final ProductSearchService search;
    private final CatalogService catalog;
    private final BrandRepository brands;
    private final SupplierRepository suppliers;
    private final FileStorageService storage;
    private final ImportService importer;
    private final InventoryMovementRepository movements;

    // ---------------- products
    @GetMapping("/products")
    public PageDto<AdminProductDto> products(@RequestParam(required = false) String q, @RequestParam(required = false) Long categoryId,
                                             @RequestParam(required = false) List<Long> brandIds,
                                             @RequestParam(defaultValue = "newest") String sort,
                                             @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return search.search(new ProductSearchService.Criteria(q, categoryId, brandIds, null, null, null, null, sort, true),
            page, size, AdminProductDto::of);
    }

    @GetMapping("/products/{id}")
    public AdminProductDto product(@PathVariable Long id) { return service.get(id); }

    @PostMapping("/products")
    public AdminProductDto create(@Valid @RequestBody ProductUpsert req) { return service.create(req, CurrentUser.id()); }

    @PutMapping("/products/{id}")
    public AdminProductDto update(@PathVariable Long id, @Valid @RequestBody ProductUpsert req) { return service.update(id, req, CurrentUser.id()); }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/products/{id}/active")
    public AdminProductDto active(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        return service.setActive(id, Boolean.TRUE.equals(body.get("active")));
    }

    @PostMapping(value = "/products/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminProductDto uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return service.addImage(id, storage.storeImage(file));
    }

    @DeleteMapping("/products/{id}/images/{imageId}")
    public AdminProductDto deleteImage(@PathVariable Long id, @PathVariable Long imageId) { return service.removeImage(id, imageId); }

    @PostMapping(value = "/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) { return Map.of("url", storage.storeImage(file)); }

    @PostMapping(value = "/products/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResult importProducts(@RequestParam("file") MultipartFile file) { return importer.importFile(file, CurrentUser.id()); }

    @GetMapping("/products/import/template")
    public ResponseEntity<String> template() {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=gola-products-template.csv")
            .contentType(MediaType.parseMediaType("text/csv")).body(importer.template());
    }

    // ---------------- categories
    @GetMapping("/categories")
    public List<CategoryDto> categories() { return catalog.categoryTree(true); }

    @PostMapping("/categories")
    public Category createCategory(@Valid @RequestBody CategoryUpsert req) { return service.saveCategory(null, req); }

    @PutMapping("/categories/{id}")
    public Category updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryUpsert req) { return service.saveCategory(id, req); }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        service.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    // ---------------- brands & suppliers
    @GetMapping("/brands")
    public List<Brand> brands() { return brands.findAllByOrderByNameAsc(); }

    @PostMapping("/brands")
    @Transactional
    public Brand createBrand(@Valid @RequestBody BrandUpsert req) {
        if (brands.findByNameIgnoreCase(req.name().trim()).isPresent()) throw ApiException.conflict("Brand already exists");
        Brand b = new Brand();
        b.setName(req.name().trim());
        b.setLogoUrl(req.logoUrl());
        b.setActive(req.active());
        return brands.save(b);
    }

    @PutMapping("/brands/{id}")
    @Transactional
    public Brand updateBrand(@PathVariable Long id, @Valid @RequestBody BrandUpsert req) {
        Brand b = brands.findById(id).orElseThrow(() -> ApiException.notFound("Brand"));
        b.setName(req.name().trim());
        b.setLogoUrl(req.logoUrl());
        b.setActive(req.active());
        return brands.save(b);
    }

    @GetMapping("/suppliers")
    public List<Supplier> suppliers() { return suppliers.findAllByOrderByNameAsc(); }

    @PostMapping("/suppliers")
    @Transactional
    public Supplier createSupplier(@Valid @RequestBody SupplierUpsert req) { return suppliers.save(apply(new Supplier(), req)); }

    @PutMapping("/suppliers/{id}")
    @Transactional
    public Supplier updateSupplier(@PathVariable Long id, @Valid @RequestBody SupplierUpsert req) {
        return suppliers.save(apply(suppliers.findById(id).orElseThrow(() -> ApiException.notFound("Supplier")), req));
    }

    private Supplier apply(Supplier s, SupplierUpsert r) {
        s.setName(r.name().trim());
        s.setContactName(r.contactName());
        s.setPhone(r.phone());
        s.setEmail(r.email());
        s.setGstin(r.gstin());
        s.setAddress(r.address());
        s.setActive(r.active());
        return s;
    }

    // ---------------- inventory
    @GetMapping("/inventory")
    public List<InventoryRow> inventory(@RequestParam(required = false) String q, @RequestParam(defaultValue = "false") boolean lowStock) {
        return service.inventory(q, lowStock);
    }

    @PostMapping("/inventory/{variantId}/adjust")
    public Inventory adjust(@PathVariable Long variantId, @Valid @RequestBody StockAdjust req) {
        return service.adjustStock(variantId, req, CurrentUser.id());
    }

    @PutMapping("/inventory/{variantId}/settings")
    public Inventory settings(@PathVariable Long variantId, @Valid @RequestBody StockSettings req) {
        return service.setThreshold(variantId, req.lowStockThreshold());
    }

    @GetMapping("/inventory/{variantId}/movements")
    @Transactional(readOnly = true)
    public PageDto<InventoryMovement> movements(@PathVariable Long variantId, @RequestParam(defaultValue = "0") int page) {
        return PageDto.of(movements.findByVariantIdOrderByCreatedAtDesc(variantId, org.springframework.data.domain.PageRequest.of(page, 50)), m -> m);
    }
}
