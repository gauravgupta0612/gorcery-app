package com.golagrocery.catalog;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Entity
@Table(name = "products")
@Getter @Setter
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String nameHi;
    private String slug;
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    private Long supplierId;
    private String hsnCode;
    private BigDecimal gstRate = BigDecimal.ZERO;
    private boolean taxInclusive = true;
    private boolean isFeatured;
    private boolean active = true;
    private BigDecimal avgRating = BigDecimal.ZERO;
    private int ratingCount;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 64)
    private List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @BatchSize(size = 64)
    private List<ProductImage> images = new ArrayList<>();

    @PreUpdate
    void touch() { updatedAt = Instant.now(); }

    public List<ProductVariant> activeVariants() {
        return variants.stream().filter(ProductVariant::isActive)
            .sorted(Comparator.comparing(ProductVariant::isDefault).reversed().thenComparing(ProductVariant::getSellingPrice))
            .toList();
    }

    public String primaryImage() {
        return images.isEmpty() ? null : images.get(0).getUrl();
    }
}
