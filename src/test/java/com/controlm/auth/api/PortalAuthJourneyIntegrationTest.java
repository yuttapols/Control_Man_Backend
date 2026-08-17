package com.controlm.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.controlm.iam.domain.UserStatus;
import com.controlm.iam.infrastructure.persistence.AppUserEntity;
import com.controlm.iam.infrastructure.persistence.AppUserJpaRepository;
import com.controlm.testsupport.PostgresIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@Tag("db")
@SpringBootTest
@AutoConfigureMockMvc
class PortalAuthJourneyIntegrationTest extends PostgresIntegrationTest {
    private static final String ORIGIN = "http://localhost:4200";
    private static final String USER_AGENT = "phase-one-integration-client";

    @Autowired private MockMvc mvc;
    @Autowired private AppUserJpaRepository users;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbc;

    @AfterEach
    void removeSyntheticRows() {
        String ids = "select id from app_user where username like 'phase1-%'";
        jdbc.update("delete from audit_log where actor_user_id in (" + ids + ")");
        jdbc.update("delete from auth_session where user_id in (" + ids + ")");
        jdbc.update("delete from user_role where user_id in (" + ids + ")");
        jdbc.update("delete from app_user where username like 'phase1-%'");
    }

    @Test
    @DisplayName("login, me, refresh rotation and logout complete against PostgreSQL")
    void completePortalAuthenticationJourney() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String username = "phase1-" + suffix;
        String password = "Synthetic-" + suffix;
        AppUserEntity user = new AppUserEntity(
                username,
                username + "@example.test",
                "Phase One Integration",
                passwordEncoder.encode(password));
        user.setStatus(UserStatus.ACTIVE);
        users.saveAndFlush(user);

        var login = mvc.perform(post("/api/v1/portal/auth/login")
                        .header("User-Agent", USER_AGENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.username").value(username))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.csrfToken").isNotEmpty())
                .andReturn();

        String accessToken = com.jayway.jsonpath.JsonPath.read(
                login.getResponse().getContentAsString(), "$.data.accessToken");
        String csrf = com.jayway.jsonpath.JsonPath.read(
                login.getResponse().getContentAsString(), "$.data.csrfToken");
        Cookie refreshCookie = login.getResponse().getCookie(AuthCookieService.REFRESH_COOKIE);
        Cookie csrfCookie = login.getResponse().getCookie(AuthCookieService.CSRF_COOKIE);
        assertThat(refreshCookie).isNotNull();
        assertThat(csrfCookie).isNotNull();

        mvc.perform(get("/api/v1/portal/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(username));

        var refresh = mvc.perform(post("/api/v1/portal/auth/refresh")
                        .header("Origin", ORIGIN)
                        .header("User-Agent", USER_AGENT)
                        .header("X-CSRF-Token", csrf)
                        .cookie(refreshCookie, csrfCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        String rotatedCsrf = com.jayway.jsonpath.JsonPath.read(
                refresh.getResponse().getContentAsString(), "$.data.csrfToken");
        Cookie rotatedRefresh = refresh.getResponse().getCookie(AuthCookieService.REFRESH_COOKIE);
        Cookie rotatedCsrfCookie = refresh.getResponse().getCookie(AuthCookieService.CSRF_COOKIE);

        mvc.perform(post("/api/v1/portal/auth/logout")
                        .header("Origin", ORIGIN)
                        .header("User-Agent", USER_AGENT)
                        .header("X-CSRF-Token", rotatedCsrf)
                        .cookie(rotatedRefresh, rotatedCsrfCookie))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/portal/auth/refresh")
                        .header("Origin", ORIGIN)
                        .header("User-Agent", USER_AGENT)
                        .header("X-CSRF-Token", rotatedCsrf)
                        .cookie(rotatedRefresh, rotatedCsrfCookie))
                .andExpect(status().isUnauthorized());
    }
}
