package com.golagrocery.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BrandRepository extends JpaRepository<Brand, Long> {
    Optional<Brand> findByNameIgnoreCase(String name);
    List<Brand> findAllByOrderByNameAsc();

    @Query(value = """
        select distinct b.* from brands b join products p on p.brand_id = b.id
        where p.active and (p.category_id = :categoryId or p.category_id in (select id from categories where parent_id = :categoryId))
        order by b.name
        """, nativeQuery = true)
    List<Brand> findForCategory(Long categoryId);
}
