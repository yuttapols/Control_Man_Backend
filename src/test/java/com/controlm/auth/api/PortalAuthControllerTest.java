package com.controlm.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.controlm.auth.application.AuthResult;
import com.controlm.auth.application.PortalAuthService;
import com.controlm.auth.application.RefreshTokenResult;
import com.controlm.auth.application.UserProfile;
import com.controlm.iam.domain.AuthenticatedUser;
import com.controlm.shared.error.GlobalExceptionHandler;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PortalAuthControllerTest {
    private PortalAuthService auth;
    private MockMvc mvc;
    private PortalAuthController controller;

    @BeforeEach
    void setUp() {
        auth = mock(PortalAuthService.class);
        AuthCookieService cookies = new AuthCookieService(
                new AuthWebProperties("example.test", true, List.of("https://portal.example.test")));
        controller = new PortalAuthController(auth, cookies);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("login คืน access token ใน envelope และ refresh token เฉพาะ HttpOnly cookie")
    void loginReturnsAccessTokenAndSecureRefreshCookie() throws Exception {
        UUID userId = UUID.randomUUID();
        var user = new AuthenticatedUser(userId, "alice", "Alice");
        var refresh = new RefreshTokenResult(
                "raw-refresh-secret", UUID.randomUUID(), userId, Instant.now().plusSeconds(3600));
        when(auth.login(anyString(), anyString(), anyString()))
                .thenReturn(new AuthResult("signed-access", 900, refresh, user, List.of("holiday.revision.submit")));

        var result = mvc.perform(post("/api/v1/portal/auth/login")
                        .header("User-Agent", "test-browser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"secret-value\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("signed-access"))
                .andExpect(jsonPath("$.data.expiresIn").value(900))
                .andExpect(jsonPath("$.data.csrfToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.username").value("alice"))
                .andExpect(jsonPath("$.data.user.permissions[0]").value("holiday.revision.submit"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("raw-refresh-secret");
        // csrfToken in the body must equal the value set in the readable csrf cookie (double-submit).
        String csrfCookie = result.getResponse().getCookie(AuthCookieService.CSRF_COOKIE).getValue();
        assertThat(com.jayway.jsonpath.JsonPath.<String>read(body, "$.data.csrfToken")).isEqualTo(csrfCookie);

        List<String> setCookies = result.getResponse().getHeaders("Set-Cookie");
        // Refresh cookie stays narrowly scoped and HttpOnly.
        assertThat(setCookies)
                .anyMatch(value -> value.startsWith("control_m_refresh=raw-refresh-secret")
                        && value.contains("Path=/api/v1/portal/auth")
                        && value.contains("HttpOnly")
                        && value.contains("Secure")
                        && value.contains("SameSite=None")
                        && !value.contains("password"));
        // CSRF cookie must be readable on every SPA route → Path=/ and not HttpOnly.
        assertThat(setCookies)
                .anyMatch(value -> value.startsWith("control_m_csrf=")
                        && value.contains("Path=/")
                        && !value.contains("Path=/api/v1/portal/auth")
                        && !value.contains("HttpOnly"));
    }

    @Test
    @DisplayName("login request ว่างถูกปฏิเสธ 400 ก่อนเรียก authentication")
    void invalidLoginRequestIsRejected() throws Exception {
        mvc.perform(post("/api/v1/portal/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("refresh rotate cookie และไม่คืน refresh token ใน response body")
    void refreshRotatesCookieWithoutLeakingToken() throws Exception {
        UUID userId = UUID.randomUUID();
        var user = new AuthenticatedUser(userId, "alice", "Alice");
        var refresh = new RefreshTokenResult(
                "new-refresh-secret", UUID.randomUUID(), userId, Instant.now().plusSeconds(3600));
        when(auth.refresh(anyString(), anyString()))
                .thenReturn(new AuthResult("new-access", 900, refresh, user, List.of("holiday.revision.submit")));

        var result = mvc.perform(post("/api/v1/portal/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie(AuthCookieService.REFRESH_COOKIE, "old-refresh"))
                        .header("User-Agent", "test-browser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access"))
                .andExpect(jsonPath("$.data.csrfToken").isNotEmpty())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("new-refresh-secret");
        assertThat(result.getResponse().getHeaders("Set-Cookie").getFirst())
                .contains("control_m_refresh=new-refresh-secret", "HttpOnly");
    }

    @Test
    @DisplayName("logout ไม่มี refresh cookie ก็สำเร็จและส่ง cookie หมดอายุกลับ")
    void logoutIsIdempotentAndClearsCookies() throws Exception {
        var result = mvc.perform(post("/api/v1/portal/auth/logout"))
                .andExpect(status().isOk())
                .andReturn();

        verify(auth).logout(null);
        // Each clearing cookie must use the same Path it was set with, or the browser keeps it.
        assertThat(result.getResponse().getHeaders("Set-Cookie"))
                .anyMatch(value -> value.contains("control_m_refresh=")
                        && value.contains("Max-Age=0")
                        && value.contains("Path=/api/v1/portal/auth"))
                .anyMatch(value -> value.contains("control_m_csrf=")
                        && value.contains("Max-Age=0")
                        && value.contains("Path=/")
                        && !value.contains("Path=/api/v1/portal/auth"));
    }

    @Test
    @DisplayName("me ใช้ subject จาก JWT โหลดข้อมูลผู้ใช้ล่าสุดพร้อม permissions ปัจจุบัน")
    void meReturnsCurrentUserWithPermissions() throws Exception {
        UUID userId = UUID.randomUUID();
        when(auth.profile(userId)).thenReturn(new UserProfile(
                new AuthenticatedUser(userId, "alice", "Alice"),
                List.of("holiday.revision.submit", "holiday.revision.view")));
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(userId.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(900))
                .build();

        UserResponse response = controller.me(jwt).data();
        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.permissions())
                .containsExactly("holiday.revision.submit", "holiday.revision.view");
    }
}
