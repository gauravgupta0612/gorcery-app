package com.golagrocery.catalog;

import com.golagrocery.catalog.CatalogDtos.*;
import com.golagrocery.common.PageDto;
import com.golagrocery.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class CatalogController {
    private final CatalogService catalog;
    private final ProductSearchService search;

    @GetMapping("/home")
    public HomeDto home() { return catalog.home(); }

    @GetMapping("/categories")
    public List<CategoryDto> categories() { return catalog.categoryTree(false); }

    @GetMapping("/brands")
    public List<BrandDto> brands(@RequestParam(required = false) Long categoryId) { return catalog.brands(categoryId); }

    @GetMapping("/products")
    public PageDto<ProductCardDto> products(@RequestParam(required = false) String q,
                                            @RequestParam(required = false) Long categoryId,
                                            @RequestParam(required = false) List<Long> brandIds,
                                            @RequestParam(required = false) BigDecimal minPrice,
                                            @RequestParam(required = false) BigDecimal maxPrice,
                                            @RequestParam(required = false) Boolean inStock,
                                            @RequestParam(required = false) Boolean discounted,
                                            @RequestParam(defaultValue = "relevance") String sort,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        if (q != null && q.length() > 100) q = q.substring(0, 100);
        return search.search(new ProductSearchService.Criteria(q, categoryId, brandIds, minPrice, maxPrice, inStock, discounted, sort, false),
            page, size, ProductCardDto::of);
    }

    @GetMapping("/products/{id}")
    public ProductDetailDto product(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        return catalog.product(id, user == null ? null : user.id());
    }
}
