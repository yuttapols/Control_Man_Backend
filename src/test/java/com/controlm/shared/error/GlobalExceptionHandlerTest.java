package com.controlm.shared.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.controlm.shared.web.RequestIdFilter;
import com.controlm.shared.web.RequestIdHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Drives the error contract through a real dispatcher without touching Spring Boot
 * autoconfiguration or a database, so the contract stays verifiable on its own.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new RequestIdFilter())
                .build();
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("business rule ที่ถูกละเมิดคืน 422 พร้อม code ที่ frontend ใช้เลือกข้อความได้")
    void businessRuleViolationReturns422() throws Exception {
        mockMvc.perform(get("/boom/business-rule"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    @DisplayName("การกระทำที่ขัดกับ state ปัจจุบันคืน 409 ไม่ใช่ 400")
    void stateConflictReturns409() throws Exception {
        mockMvc.perform(get("/boom/state-conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STATE_CONFLICT"));
    }

    @Test
    @DisplayName("แก้ข้อมูลที่ถูกคนอื่นแก้ไปแล้วคืน 409 optimistic lock พร้อมบอกให้โหลดใหม่")
    void optimisticLockReturns409() throws Exception {
        mockMvc.perform(get("/boom/optimistic-lock"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OPTIMISTIC_LOCK_CONFLICT"));
    }

    @Test
    @DisplayName("resource ที่ไม่มีหรือที่ต้องซ่อนคืน 404 เหมือนกัน ไม่บอกใบ้ว่ามีอยู่จริง")
    void notFoundReturns404() throws Exception {
        mockMvc.perform(get("/boom/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("ผู้ใช้ที่ยืนยันตัวตนแล้วแต่ไม่มีสิทธิ์คืน 403 แยกจาก 401")
    void accessDeniedReturns403() throws Exception {
        mockMvc.perform(get("/boom/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("การยืนยันตัวตนล้มเหลวคืน 401 ด้วยข้อความกลาง ไม่เปิดเผยว่า username มีอยู่จริงหรือไม่")
    void authenticationFailureReturnsGeneric401() throws Exception {
        mockMvc.perform(get("/boom/bad-credentials"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.detail").value("Authentication required"));
    }

    @Test
    @DisplayName("ข้อผิดพลาดจากฐานข้อมูลไม่รั่วชื่อ constraint หรือ SQL ออกไปหา client")
    void dataIntegrityViolationDoesNotLeakDatabaseDetail() throws Exception {
        mockMvc.perform(get("/boom/data-integrity"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"))
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    if (body.contains("uq_app_user_username_ci") || body.toLowerCase().contains("insert into")) {
                        throw new AssertionError("error response leaked database detail: " + body);
                    }
                });
    }

    @Test
    @DisplayName("exception ที่ไม่คาดคิดคืน 500 โดยไม่มี stack trace หรือชื่อ class ภายในหลุดออกไป")
    void unexpectedExceptionIsSanitised() throws Exception {
        mockMvc.perform(get("/boom/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    if (body.contains("java.lang") || body.contains("com.controlm") || body.contains("at ")) {
                        throw new AssertionError("error response leaked internals: " + body);
                    }
                });
    }

    @Test
    @DisplayName("ทุก error response แนบ requestId ตัวเดียวกับที่อยู่ใน response header")
    void everyErrorCarriesTheRequestId() throws Exception {
        mockMvc.perform(get("/boom/not-found").header(RequestIdHolder.HEADER, "trace-0001"))
                .andExpect(jsonPath("$.requestId").value("trace-0001"))
                .andExpect(result ->
                        org.assertj.core.api.Assertions.assertThat(result.getResponse().getHeader(RequestIdHolder.HEADER))
                                .isEqualTo("trace-0001"));
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/boom/business-rule")
        void businessRule() {
            throw new BusinessRuleException("Substitute holiday must reference a source holiday");
        }

        @GetMapping("/boom/state-conflict")
        void stateConflict() {
            throw new StateConflictException("Revision is no longer pending level 1");
        }

        @GetMapping("/boom/optimistic-lock")
        void optimisticLock() {
            throw new OptimisticLockingFailureException("stale version");
        }

        @GetMapping("/boom/not-found")
        void notFound() {
            throw new ResourceNotFoundException("Holiday not found");
        }

        @GetMapping("/boom/access-denied")
        void accessDenied() {
            throw new AccessDeniedException("no permission");
        }

        @GetMapping("/boom/bad-credentials")
        void badCredentials() {
            throw new BadCredentialsException("user somchai does not exist");
        }

        @GetMapping("/boom/data-integrity")
        void dataIntegrity() {
            throw new DataIntegrityViolationException(
                    "duplicate key value violates unique constraint \"uq_app_user_username_ci\"");
        }

        @GetMapping("/boom/unexpected")
        void unexpected() {
            throw new IllegalStateException("connection string jdbc:postgresql://host/db?password=secret");
        }
    }
}
