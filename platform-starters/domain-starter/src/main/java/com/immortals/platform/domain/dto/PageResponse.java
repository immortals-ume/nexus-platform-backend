package com.immortals.platform.domain.dto;

import java.util.List;

/**
 * Page response DTO using Java 21 Record.
 * Provides immutable pagination response with metadata.
 *
 * @param <T> The type of content in the page
 */
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last,
    boolean hasNext,
    boolean hasPrevious
) {
    /**
     * Compact constructor with validation and computed fields
     */
    public PageResponse {
        if (content == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }
        if (page < 0) {
            throw new IllegalArgumentException("Page number must be >= 0");
        }
        if (size < 0) {
            throw new IllegalArgumentException("Page size must be >= 0");
        }
        if (totalElements < 0) {
            throw new IllegalArgumentException("Total elements must be >= 0");
        }
        if (totalPages < 0) {
            throw new IllegalArgumentException("Total pages must be >= 0");
        }
    }

    /**
     * Creates a page response from content and pagination metadata
     */
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        boolean first = page == 0;
        boolean last = page >= totalPages - 1;
        boolean hasNext = page < totalPages - 1;
        boolean hasPrevious = page > 0;

        return new PageResponse<>(
            content,
            page,
            size,
            totalElements,
            totalPages,
            first,
            last,
            hasNext,
            hasPrevious
        );
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
            page.isLast(),
            page.hasNext(),
            page.hasPrevious()
        );
    }

    /**
     * Create empty page response
     */
    public static <T> PageResponse<T> empty() {
        return new PageResponse<>(List.of(), 0, 0, 0, 0, true, true, false, false);
    }

    /**
     * Creates an empty page response with specific page and size
     */
    public static <T> PageResponse<T> empty(int page, int size) {
        return new PageResponse<>(List.of(), page, size, 0, 0, true, true, false, false);
    }

    /**
     * Check if page has content
     */
    public boolean hasContent() {
        return !content.isEmpty();
    }
}
