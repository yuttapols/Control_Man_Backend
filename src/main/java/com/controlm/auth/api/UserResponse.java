package com.controlm.auth.api;

import com.controlm.iam.domain.AuthenticatedUser;
import java.util.List;
import java.util.UUID;

public record UserResponse(UUID id, String username, String displayName, List<String> permissions) {
    static UserResponse from(AuthenticatedUser user, List<String> permissions) {
        return new UserResponse(user.id(), user.username(), user.displayName(), List.copyOf(permissions));
    }
}
