package com.golagrocery.catalog;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "categories")
@Getter @Setter
public class Category {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long parentId;
    private String name;
    private String nameHi;
    private String slug;
    private String imageUrl;
    private int sortOrder;
    private boolean active = true;
}
