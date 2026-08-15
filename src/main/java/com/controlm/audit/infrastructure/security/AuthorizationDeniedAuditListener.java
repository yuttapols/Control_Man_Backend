package com.controlm.audit.infrastructure.security;

import com.controlm.audit.application.AuditCommand;
import com.controlm.audit.application.AuditService;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class AuthorizationDeniedAuditListener {
    private final AuditService audit;

    public AuthorizationDeniedAuditListener(AuditService audit) {
        this.audit = audit;
    }

    @EventListener
    public void onDenied(AuthorizationDeniedEvent<?> event) {
        var authentication = event.getAuthentication().get();
        UUID actorId = null;
        if (authentication instanceof JwtAuthenticationToken jwt) {
            try { actorId = UUID.fromString(jwt.getToken().getSubject()); }
            catch (IllegalArgumentException ignored) { actorId = null; }
        }
        String target = "protected-resource";
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            var request = attributes.getRequest();
            target = request.getMethod() + " " + request.getRequestURI();
        }
        AuditCommand command = actorId == null
                ? AuditCommand.system("auth", "AUTHORIZATION_DENIED", "REQUEST", target,
                        Map.of("outcome", "DENIED"))
                : AuditCommand.user(actorId, "auth", "AUTHORIZATION_DENIED", "REQUEST", target,
                        Map.of("outcome", "DENIED"));
        audit.record(command);
    }
}
