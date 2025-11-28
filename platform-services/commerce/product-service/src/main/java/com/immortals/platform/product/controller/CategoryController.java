package com.immortals.platform.product.controller;

import com.immortals.platform.domain.dto.ApiResponse;
import com.immortals.platform.domain.dto.product.CreateCategoryRequest;
import com.immortals.platform.domain.dto.product.UpdateCategoryRequest;
import com.immortals.platform.domain.entity.Category;
import com.immortals.platform.product.service.ICategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Category operations.
 * Provides endpoints for category CRUD operations.
 */
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Category Management", description = "APIs for managing product categories")
public class CategoryController {

    private final ICategoryService categoryService;

    @PostMapping
    @Operation(summary = "Create a new category", description = "Creates a new product category")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Category created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Category with slug already exists")
    })
    public ResponseEntity<ApiResponse<Category>> createCategory(
            @Valid @RequestBody CreateCategoryRequest request) {
        
        Category category = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(category, "Category created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID", description = "Retrieves a category by its unique identifier")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Category found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category not found")
    })
    public ResponseEntity<ApiResponse<Category>> getCategoryById(
            @Parameter(description = "Category ID") @PathVariable UUID id) {
        
        Category category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success(category, "Category retrieved successfully"));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get category by slug", description = "Retrieves a category by its slug")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Category found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category not found")
    })
    public ResponseEntity<ApiResponse<Category>> getCategoryBySlug(
            @Parameter(description = "Category slug") @PathVariable String slug) {
        
        Category category = categoryService.getCategoryBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(category, "Category retrieved successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update category", description = "Updates an existing category")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Category updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category not found")
    })
    public ResponseEntity<ApiResponse<Category>> updateCategory(
            @Parameter(description = "Category ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        
        Category category = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success(category, "Category updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete category", description = "Soft deletes a category")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Category deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Category has children")
    })
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @Parameter(description = "Category ID") @PathVariable UUID id,
            @Parameter(description = "User performing deletion") @RequestParam(required = false, defaultValue = "system") String deletedBy) {
        
        categoryService.deleteCategory(id, deletedBy);
        return ResponseEntity.ok(ApiResponse.message("Category deleted successfully"));
    }

    @GetMapping("/root")
    @Operation(summary = "Get root categories", description = "Retrieves all root categories (categories without parent)")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Root categories retrieved successfully")
    })
    public ResponseEntity<ApiResponse<List<Category>>> getRootCategories() {
        
        List<Category> categories = categoryService.getAllRootCategories();
        return ResponseEntity.ok(ApiResponse.success(categories, "Root categories retrieved successfully"));
    }

    @GetMapping("/{id}/children")
    @Operation(summary = "Get category children", description = "Retrieves all child categories of a parent category")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Child categories retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Parent category not found")
    })
    public ResponseEntity<ApiResponse<List<Category>>> getCategoryChildren(
            @Parameter(description = "Parent category ID") @PathVariable UUID id) {
        
        List<Category> categories = categoryService.getCategoryChildren(id);
        return ResponseEntity.ok(ApiResponse.success(categories, "Child categories retrieved successfully"));
    }

    @GetMapping("/active")
    @Operation(summary = "Get active categories", description = "Retrieves all active categories")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Active categories retrieved successfully")
    })
    public ResponseEntity<ApiResponse<List<Category>>> getAllActiveCategories() {
        
        List<Category> categories = categoryService.getAllActiveCategories();
        return ResponseEntity.ok(ApiResponse.success(categories, "Active categories retrieved successfully"));
    }

    @GetMapping("/leaf")
    @Operation(summary = "Get leaf categories", description = "Retrieves all leaf categories (categories without children)")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Leaf categories retrieved successfully")
    })
    public ResponseEntity<ApiResponse<List<Category>>> getLeafCategories() {
        
        List<Category> categories = categoryService.getLeafCategories();
        return ResponseEntity.ok(ApiResponse.success(categories, "Leaf categories retrieved successfully"));
    }
}