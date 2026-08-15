package com.controlm.auth.application;

import com.controlm.iam.domain.AuthenticatedUser;
import java.util.UUID;

/** Issues short-lived access tokens for authenticated Portal sessions. */
public interface AccessTokenService {
    String issue(AuthenticatedUser user, UUID sessionId, long authorizationVersion);
}
