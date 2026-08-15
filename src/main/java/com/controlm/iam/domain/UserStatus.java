package com.controlm.iam.domain;

/**
 * Account state of an {@code app_user}. Values mirror the
 * {@code CHECK (status IN ('INVITED', 'ACTIVE', 'LOCKED', 'SUSPENDED', 'DISABLED'))} constraint
 * in Flyway V1. Only {@link #ACTIVE} accounts may authenticate.
 */
public enum UserStatus {
    INVITED,
    ACTIVE,
    LOCKED,
    SUSPENDED,
    DISABLED
}
