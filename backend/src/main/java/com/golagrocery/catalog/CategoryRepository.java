package com.golagrocery.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByOrderBySortOrderAscNameAsc();
    List<Category> findByActiveTrueOrderBySortOrderAscNameAsc();
    Optional<Category> findBySlug(String slug);
    boolean existsBySlug(String slug);
    long countByParentId(Long parentId);
}
