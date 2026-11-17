package com.golagrocery.catalog;

import com.golagrocery.common.PageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Search & filter with whitelisted sorting. All user input is bound as parameters (no SQL injection). */
@Service
@RequiredArgsConstructor
public class ProductSearchService {
    private final NamedParameterJdbcTemplate jdbc;
    private final ProductRepository products;

    public record Criteria(String q, Long categoryId, List<Long> brandIds, BigDecimal minPrice, BigDecimal maxPrice,
                           Boolean inStockOnly, Boolean discountedOnly, String sort, boolean includeInactive) {}

    private static final Map<String, String> SORTS = Map.of(
        "relevance", "p.is_featured desc, p.rating_count desc, p.id",
        "price_asc", "dv.selling_price asc, p.id",
        "price_desc", "dv.selling_price desc, p.id",
        "discount", "((dv.mrp - dv.selling_price) / dv.mrp) desc, p.id",
        "rating", "p.avg_rating desc, p.rating_count desc, p.id",
        "newest", "p.created_at desc, p.id desc",
        "name", "p.name asc, p.id");

    @Transactional(readOnly = true)
    public <T> PageDto<T> search(Criteria c, int page, int size, Function<Product, T> mapper) {
        size = Math.min(Math.max(size, 1), 100);
        page = Math.max(page, 0);
        StringBuilder where = new StringBuilder(" where 1=1");
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (!c.includeInactive()) where.append(" and p.active");
        if (StringUtils.hasText(c.q())) {
            where.append(" and (p.name ilike :q or coalesce(p.name_hi,'') ilike :q or coalesce(b.name,'') ilike :q or coalesce(p.description,'') ilike :q")
                 .append(" or exists (select 1 from product_variants sv where sv.product_id = p.id and (sv.sku ilike :q or sv.barcode = :rawq)))");
            String safe = c.q().trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            params.addValue("q", "%" + safe + "%");
            params.addValue("rawq", c.q().trim());
        }
        if (c.categoryId() != null) {
            where.append(" and (p.category_id = :cat or p.category_id in (select id from categories where parent_id = :cat))");
            params.addValue("cat", c.categoryId());
        }
        if (c.brandIds() != null && !c.brandIds().isEmpty()) {
            where.append(" and p.brand_id in (:brands)");
            params.addValue("brands", c.brandIds());
        }
        if (c.minPrice() != null) { where.append(" and dv.selling_price >= :minp"); params.addValue("minp", c.minPrice()); }
        if (c.maxPrice() != null) { where.append(" and dv.selling_price <= :maxp"); params.addValue("maxp", c.maxPrice()); }
        if (Boolean.TRUE.equals(c.inStockOnly())) {
            where.append(" and exists (select 1 from product_variants iv join inventory i on i.variant_id = iv.id where iv.product_id = p.id and iv.active and i.quantity > 0)");
        }
        if (Boolean.TRUE.equals(c.discountedOnly())) where.append(" and dv.selling_price < dv.mrp");

        String from = " from products p left join brands b on b.id = p.brand_id" +
            " left join lateral (select v.selling_price, v.mrp from product_variants v where v.product_id = p.id and v.active" +
            " order by v.is_default desc, v.selling_price asc limit 1) dv on true";
        String order = SORTS.getOrDefault(c.sort() == null ? "relevance" : c.sort(), SORTS.get("relevance"));

        Long total = jdbc.queryForObject("select count(*)" + from + where, params, Long.class);
        params.addValue("limit", size).addValue("offset", (long) page * size);
        List<Long> ids = jdbc.queryForList("select p.id" + from + where + " order by " + order + " limit :limit offset :offset",
            params, Long.class);

        Map<Long, Product> byId = products.findAllById(ids).stream().collect(Collectors.toMap(Product::getId, p -> p));
        List<T> items = ids.stream().map(byId::get).filter(Objects::nonNull).map(mapper).toList();
        long t = total == null ? 0 : total;
        return new PageDto<>(items, page, size, t, (int) Math.ceil(t / (double) size));
    }
}
