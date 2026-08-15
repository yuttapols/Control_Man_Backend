package com.controlm.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.controlm.auth.application.PortalAuthorization;
import com.controlm.auth.application.PortalAuthorizationService;
import com.controlm.audit.application.AuditService;
import com.controlm.shared.error.ApiException;
import com.controlm.shared.error.ErrorCode;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class PortalAuthorizationFilterTest {
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("permission codes ถูกติดตั้งเป็น authorities ใน SecurityContext")
    void permissionsBecomeAuthorities() throws Exception {
        PortalAuthorizationService service = mock(PortalAuthorizationService.class);
        Jwt jwt = jwt();
        when(service.authorize(jwt)).thenReturn(new PortalAuthorization(
                "alice", Set.of("holiday.revision.create", "audit.log.read")));
        SecurityContextHolder.getContext().setAuthentication(authenticated(jwt));
        MockFilterChain chain = new MockFilterChain();

        new PortalAuthorizationFilter(service, mock(AuditService.class)).doFilter(
                new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("holiday.revision.create", "audit.log.read");
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("inactive user หรือ revoked session ถูกล้าง identity และคืน 401")
    void invalidCurrentIdentityReturns401() throws Exception {
        PortalAuthorizationService service = mock(PortalAuthorizationService.class);
        Jwt jwt = jwt();
        when(service.authorize(jwt)).thenThrow(new ApiException(ErrorCode.UNAUTHENTICATED, "Authentication required"));
        SecurityContextHolder.getContext().setAuthentication(authenticated(jwt));
        MockHttpServletResponse response = new MockHttpServletResponse();

        new PortalAuthorizationFilter(service, mock(AuditService.class)).doFilter(
                new MockHttpServletRequest(), response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private static Jwt jwt() {
        Instant now = Instant.now();
        return Jwt.withTokenValue("token").header("alg", "RS256").subject(UUID.randomUUID().toString())
                .claim("sid", UUID.randomUUID().toString()).issuedAt(now).expiresAt(now.plusSeconds(900)).build();
    }

    private static JwtAuthenticationToken authenticated(Jwt jwt) {
        return new JwtAuthenticationToken(jwt, java.util.List.of(new SimpleGrantedAuthority("authenticated")));
    }
}
