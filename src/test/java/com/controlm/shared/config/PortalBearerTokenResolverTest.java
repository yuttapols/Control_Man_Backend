package com.controlm.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class PortalBearerTokenResolverTest {
    private final PortalBearerTokenResolver resolver = new PortalBearerTokenResolver();

    @Test
    @DisplayName("refresh ที่แนบ access token หมดอายุมาด้วย ต้องไม่ถูก resolve (กัน 401 จาก bearer filter บน permitAll)")
    void refreshIgnoresPresentedBearerToken() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/portal/auth/refresh");
        request.addHeader("Authorization", "Bearer expired-access-token");

        assertThat(resolver.resolve(request)).isNull();
    }

    @Test
    @DisplayName("login และ logout ก็ไม่ resolve bearer token เช่นกัน")
    void loginAndLogoutIgnoreBearerToken() {
        for (String path : new String[] {"/api/v1/portal/auth/login", "/api/v1/portal/auth/logout"}) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
            request.addHeader("Authorization", "Bearer any-token");
            assertThat(resolver.resolve(request)).isNull();
        }
    }

    @Test
    @DisplayName("me ยังคงตรวจ JWT ตามปกติ — resolve bearer token ที่แนบมา")
    void meStillResolvesBearerToken() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/portal/auth/me");
        request.addHeader("Authorization", "Bearer valid-access-token");

        assertThat(resolver.resolve(request)).isEqualTo("valid-access-token");
    }

    @Test
    @DisplayName("endpoint ป้องกันอื่น ๆ ยัง resolve bearer token ตามเดิม")
    void protectedResourceStillResolvesBearerToken() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/portal/holidays");
        request.addHeader("Authorization", "Bearer valid-access-token");

        assertThat(resolver.resolve(request)).isEqualTo("valid-access-token");
    }

    @Test
    @DisplayName("GET ไปยัง path auth ที่ตรงชื่อ ก็ยัง resolve (ยกเว้นเฉพาะ POST login/refresh/logout เท่านั้น)")
    void nonPostToAuthPathStillResolves() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/portal/auth/refresh");
        request.addHeader("Authorization", "Bearer some-token");

        assertThat(resolver.resolve(request)).isEqualTo("some-token");
    }
}
