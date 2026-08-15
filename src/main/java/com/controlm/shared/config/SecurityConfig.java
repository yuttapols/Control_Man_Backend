package com.controlm.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import com.controlm.auth.api.AuthCsrfFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import com.controlm.auth.infrastructure.security.PortalAuthorizationFilter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Baseline security for BE1-01/BE1-02.
 *
 * <p>Deny by default: only health probes and the API documentation are open. Portal bearer JWTs
 * are signature/issuer/audience/expiry validated here. Permission checks and CSRF protection for
 * cookie-backed refresh/logout arrive with BE1-07 to BE1-09.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
        "/actuator/health",
        "/actuator/health/**",
        "/v3/api-docs",
        "/v3/api-docs/**",
        "/swagger-ui.html",
        "/swagger-ui/**"
    };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuthCsrfFilter authCsrfFilter,
            PortalAuthorizationFilter portalAuthorizationFilter) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers(PUBLIC_PATHS)
                        .permitAll()
                        .requestMatchers("/api/v1/portal/auth/login", "/api/v1/portal/auth/refresh",
                                "/api/v1/portal/auth/logout")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                // No browser login form: unauthenticated Portal calls must fail with 401,
                // never a redirect the Angular client cannot interpret.
                .exceptionHandling(handling -> handling.authenticationEntryPoint(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .addFilterBefore(authCsrfFilter, BasicAuthenticationFilter.class)
                .addFilterAfter(portalAuthorizationFilter, BearerTokenAuthenticationFilter.class)
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .build();
    }
}
