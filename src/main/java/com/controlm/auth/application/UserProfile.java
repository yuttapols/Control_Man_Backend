package com.controlm.auth.application;

import com.controlm.iam.domain.AuthenticatedUser;
import java.util.List;

/** Current user plus the permission codes that are active right now — the payload behind {@code /auth/me}. */
public record UserProfile(AuthenticatedUser user, List<String> permissions) {
    public UserProfile {
        permissions = List.copyOf(permissions);
    }
}
