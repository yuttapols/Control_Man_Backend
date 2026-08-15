package com.controlm.iam.domain;

/**
 * Lifecycle flag shared by reference-style aggregates such as {@code user_level} and {@code role}.
 * Values mirror the {@code CHECK (status IN ('ACTIVE', 'INACTIVE'))} constraint in Flyway V1.
 */
public enum ActivationStatus {
    ACTIVE,
    INACTIVE
}
