package com.immortals.platform.domain.entity;

import com.immortals.platform.domain.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Category entity representing a product category with hierarchical structure.
 * Supports self-referencing parent-child relationships for category trees.
 */
@Entity
@Table(name = "categories", indexes = {
    @Index(name = "idx_category_parent", columnList = "parent_id"),
    @Index(name = "idx_category_display_order", columnList = "display_order")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SuppressWarnings("serial")
public class Category extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Category name is required")
    @Size(max = 255, message = "Category name must not exceed 255 characters")
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = false)
    @Builder.Default
    private List<Category> children = new ArrayList<>();

    @Column(name = "display_order")
    private Integer displayOrder;

    @Size(max = 100, message = "Slug must not exceed 100 characters")
    @Column(name = "slug", unique = true, length = 100)
    private String slug;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "metadata", columnDefinition = "JSONB")
    private String metadata;

    /**
     * Check if this is a root category (no parent)
     */
    public boolean isRoot() {
        return parent == null;
    }

    /**
     * Check if this is a leaf category (no children)
     */
    public boolean isLeaf() {
        return children == null || children.isEmpty();
    }

    /**
     * Get the depth level in the category tree (0 for root)
     */
    public int getLevel() {
        int level = 0;
        Category current = this.parent;
        while (current != null) {
            level++;
            current = current.getParent();
        }
        return level;
    }

    /**
     * Add a child category
     */
    public void addChild(Category child) {
        if (children == null) {
            children = new ArrayList<>();
        }
        children.add(child);
        child.setParent(this);
    }

    /**
     * Remove a child category
     */
    public void removeChild(Category child) {
        if (children != null) {
            children.remove(child);
            child.setParent(null);
        }
    }

    /**
     * Get full category path (e.g., "Electronics > Computers > Laptops")
     */
    public String getFullPath() {
        if (isRoot()) {
            return name;
        }
        return parent.getFullPath() + " > " + name;
    }

    /**
     * Activate category
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * Deactivate category
     */
    public void deactivate() {
        this.isActive = false;
    }
}
