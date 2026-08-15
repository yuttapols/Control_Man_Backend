package com.controlm.shared.api;

import org.springframework.data.domain.Page;

/**
 * Paging block placed inside {@link ApiMeta} for list responses.
 */
public record PageMeta(int number, int size, long totalElements, int totalPages) {

    public static PageMeta from(Page<?> page) {
        return new PageMeta(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
