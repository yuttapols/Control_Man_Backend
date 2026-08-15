package com.controlm.shared.api;

/**
 * Success envelope for every Portal and published endpoint: {@code data} carries the payload
 * and {@code meta} carries request correlation and, for lists, paging.
 */
public record ApiResponse<T>(T data, ApiMeta meta) {

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data, ApiMeta.current());
    }

    public static <T> ApiResponse<T> ofPage(T data, PageMeta page) {
        return new ApiResponse<>(data, ApiMeta.current(page));
    }
}
