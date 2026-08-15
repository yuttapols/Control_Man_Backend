package com.controlm.auth.api;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthCsrfFilterTest {
    private final AuthWebProperties properties =
            new AuthWebProperties(null, true, List.of("https://portal.example.test"));
    private final AuthCookieService cookies = new AuthCookieService(properties);
    private final AuthCsrfFilter filter = new AuthCsrfFilter(properties, cookies);

    @Test
    @DisplayName("refresh ที่ Origin และ double-submit token ตรงกันผ่าน filter")
    void validOriginAndDoubleSubmitTokenPass() throws Exception {
        MockHttpServletRequest request = request("/api/v1/portal/auth/refresh");
        request.addHeader("Origin", "https://portal.example.test");
        request.addHeader(AuthCookieService.CSRF_HEADER, "csrf-value");
        request.setCookies(new Cookie(AuthCookieService.CSRF_COOKIE, "csrf-value"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("refresh จาก Origin ที่ไม่อนุญาตถูกปฏิเสธ 403")
    void untrustedOriginIsRejected() throws Exception {
        MockHttpServletRequest request = request("/api/v1/portal/auth/refresh");
        request.addHeader("Origin", "https://evil.example");
        request.addHeader(AuthCookieService.CSRF_HEADER, "csrf-value");
        request.setCookies(new Cookie(AuthCookieService.CSRF_COOKIE, "csrf-value"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("logout ที่ header CSRF ไม่ตรง cookie ถูกปฏิเสธ 403")
    void csrfMismatchIsRejected() throws Exception {
        MockHttpServletRequest request = request("/api/v1/portal/auth/logout");
        request.addHeader("Origin", "https://portal.example.test");
        request.addHeader(AuthCookieService.CSRF_HEADER, "wrong");
        request.setCookies(new Cookie(AuthCookieService.CSRF_COOKIE, "expected"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
    }

    private static MockHttpServletRequest request(String path) {
        return new MockHttpServletRequest("POST", path);
    }
}
