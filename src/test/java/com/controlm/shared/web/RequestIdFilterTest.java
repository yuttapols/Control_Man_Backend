package com.controlm.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("ทุก request ได้ request id และถูกส่งกลับใน response header เพื่อให้ผู้ใช้อ้างอิงตอนแจ้งปัญหาได้")
    void generatesAndReturnsRequestId() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/portal/holidays"), response, new MockFilterChain());

        assertThat(response.getHeader(RequestIdHolder.HEADER)).isNotBlank();
    }

    @Test
    @DisplayName("request id ที่ client ส่งมาถูกใช้ต่อ เพื่อให้ trace ข้ามระบบได้")
    void reusesCallerSuppliedRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/holidays");
        request.addHeader(RequestIdHolder.HEADER, "abc-123_45");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(RequestIdHolder.HEADER)).isEqualTo("abc-123_45");
    }

    @Test
    @DisplayName("request id ที่มีอักขระอันตรายถูกทิ้งและสร้างใหม่ ป้องกัน log injection จาก client")
    void rejectsUnsafeRequestIdFromClient() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/holidays");
        request.addHeader(RequestIdHolder.HEADER, "evil\nFAKE LOG LINE");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String returned = response.getHeader(RequestIdHolder.HEADER);
        assertThat(returned).doesNotContain("evil").doesNotContain("\n").isNotBlank();
    }

    @Test
    @DisplayName("request id ยาวเกินเพดานถูกปฏิเสธ ไม่ให้ client ยัดข้อมูลขนาดใหญ่เข้า log")
    void rejectsOverlongRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/holidays");
        request.addHeader(RequestIdHolder.HEADER, "a".repeat(65));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(RequestIdHolder.HEADER)).hasSize(32);
    }

    @Test
    @DisplayName("request id ถูกล้างออกจาก MDC เมื่อจบ request ไม่ให้ค้างไปปนกับ request ถัดไปของ thread เดิม")
    void clearsRequestIdAfterRequest() throws Exception {
        filter.doFilter(
                new MockHttpServletRequest("GET", "/api/v1/holidays"),
                new MockHttpServletResponse(),
                new MockFilterChain());

        assertThat(RequestIdHolder.currentRequestId()).isNull();
    }

    @Test
    @DisplayName("request id ถูกล้างแม้ request ล้มเหลวกลางทาง")
    void clearsRequestIdEvenWhenChainThrows() {
        MockFilterChain failingChain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) {
                throw new IllegalStateException("boom");
            }
        };

        try {
            filter.doFilter(new MockHttpServletRequest("GET", "/x"), new MockHttpServletResponse(), failingChain);
        } catch (Exception expected) {
            // the filter must still clean up
        }

        assertThat(RequestIdHolder.currentRequestId()).isNull();
    }
}
