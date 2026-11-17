package com.golagrocery.catalog;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySlug(String slug);
    boolean existsBySlug(String slug);
    long countByCategoryId(Long categoryId);

    @Query("select p from Product p where p.active = true and p.isFeatured = true order by p.ratingCount desc, p.id")
    List<Product> findFeatured(Pageable pageable);

    @Query("""
        select p from Product p where p.active = true and
        (p.category.id = :categoryId or p.category.parentId = :categoryId) order by p.isFeatured desc, p.id
        """)
    List<Product> findByCategoryTree(Long categoryId, Pageable pageable);
}
