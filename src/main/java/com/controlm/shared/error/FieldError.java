package com.controlm.shared.error;

/**
 * One entry of the {@code errors[]} array in an error response.
 */
public record FieldError(String field, String code, String message) {}
