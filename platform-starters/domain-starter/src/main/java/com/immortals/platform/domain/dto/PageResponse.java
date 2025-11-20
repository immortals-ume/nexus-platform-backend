package com.immortals.platform.domain.dto;

import java.util.List;

/**
 * Page response DTO using Java 21 Record.
 * Provides immutable pagination response with metadata.
 */
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {
    /**
     * Compact constructor with validation
     */
    public PageResponse {
        if (content == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }
        if (page < 0) {
            throw new IllegalArgumentException("Page number must be >= 0");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Page size must be > 0");
        }
        if (totalElements < 0) {
            throw new IllegalArgumentException("Total elements must be >= 0");
        }
        if (totalPages < 0) {
            throw new IllegalArgumentException("Total pages must be >= 0");
        }
    }

    /**
     * Create page response from Spring Data Page
     */
    public static <T> PageResponse<T> from(org.springframework.data.domain.Page<T> page) {
        return new PageResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast()
        );
    }

    /**
     * Create empty page response
     */
    public static <T> PageResponse<T> empty() {
        return new PageResponse<>(List.of(), 0, 0, 0, 0, true, true);
    }

    /**
     * Check if page has content
     */
    public boolean hasContent() {
        return !content.isEmpty();
    }

    /**
     * Check if there's a next page
     */
    public boolean hasNext() {
        return !last;
    }

    /**
     * Check if there's a previous page
     */
    public boolean hasPrevious() {
        return !first;
    }
}
