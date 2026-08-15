package com.controlm.auth.application;

import java.util.Set;

public record PortalAuthorization(String username, Set<String> permissions) {
    public PortalAuthorization {
        permissions = Set.copyOf(permissions);
    }
}
