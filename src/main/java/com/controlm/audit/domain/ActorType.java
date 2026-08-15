package com.controlm.audit.domain;

/**
 * Who caused an audited event. Values mirror the
 * {@code CHECK (actor_type IN ('USER', 'SYSTEM', 'API_CONSUMER'))} constraint in Flyway V1.
 */
public enum ActorType {
    USER,
    SYSTEM,
    API_CONSUMER
}
