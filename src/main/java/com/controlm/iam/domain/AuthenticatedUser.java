package com.controlm.iam.domain;

import java.util.UUID;

/**
 * Result of a successful credential check: just enough identity for the auth module to mint a
 * session. Deliberately carries no password, hash or status field.
 */
public record AuthenticatedUser(UUID id, String username, String displayName) {
}
