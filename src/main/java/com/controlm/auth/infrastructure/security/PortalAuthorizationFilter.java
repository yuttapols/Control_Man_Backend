package com.controlm.auth.infrastructure.security;

import com.controlm.auth.application.PortalAuthorizationService;
import com.controlm.audit.application.AuditCommand;
import com.controlm.audit.application.AuditService;
import com.controlm.shared.error.ApiException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class PortalAuthorizationFilter extends OncePerRequestFilter {
    private final PortalAuthorizationService authorization;
    private final AuditService audit;

    public PortalAuthorizationFilter(PortalAuthorizationService authorization, AuditService audit) {
        this.authorization = authorization;
        this.audit = audit;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication && authentication.isAuthenticated()) {
            try {
                var current = authorization.authorize(jwtAuthentication.getToken());
                var authorities = current.permissions().stream().sorted().map(SimpleGrantedAuthority::new).toList();
                SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                        jwtAuthentication.getToken(), authorities, current.username()));
            } catch (ApiException ex) {
                audit.record(AuditCommand.system("auth", "AUTHORIZATION_IDENTITY_DENIED", "REQUEST",
                        request.getMethod() + " " + request.getRequestURI(),
                        Map.of("outcome", "DENIED", "reason", "INVALID_CURRENT_IDENTITY")));
                SecurityContextHolder.clearContext();
                response.sendError(HttpStatus.UNAUTHORIZED.value());
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
