package com.controlm.shared.config;

import com.controlm.testsupport.PostgresIntegrationTest;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** Boots the whole application, so it needs PostgreSQL: run with {@code ./mvnw verify -Pdb}. */
@Tag("db")
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("คำขอที่ไม่มี identity ไปยัง path ที่ไม่ได้เปิดสาธารณะ ต้องได้ 401 ไม่ใช่ redirect ไปหน้า login")
    void unauthenticatedRequestIsRejectedWith401() throws Exception {
        mockMvc.perform(get("/api/v1/portal/anything")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Health probe เปิดให้เรียกได้โดยไม่ต้อง authenticate เพื่อให้ platform ตรวจสถานะได้")
    void healthProbeIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("liveness และ readiness probes เปิด public แต่ไม่เปิดรายละเอียด dependency")
    void probesArePublicAndSafe() throws Exception {
        for (String path : new String[] {"/actuator/health/liveness", "/actuator/health/readiness"}) {
            mockMvc.perform(get(path))
                    .andExpect(status().isOk())
                    .andExpect(result -> {
                        String body = result.getResponse().getContentAsString();
                        if (body.contains("jdbc:") || body.contains("database") || body.contains("components")) {
                            throw new AssertionError("Health response leaked dependency detail: " + body);
                        }
                    });
        }
    }

    @Test
    @DisplayName("metrics และ prometheus ไม่เปิดให้ anonymous")
    void metricsAreNotPublic() throws Exception {
        mockMvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("OpenAPI document เปิดอ่านได้ และประกาศ security scheme ทั้ง Portal JWT และ consumer API key")
    void openApiDocumentIsAvailableAndDeclaresBothSecuritySchemes() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    if (!body.contains(OpenApiConfig.PORTAL_JWT) || !body.contains(OpenApiConfig.CONSUMER_API_KEY)) {
                        throw new AssertionError("OpenAPI document is missing a declared security scheme: " + body);
                    }
                });
    }

    @Test
    @DisplayName("Actuator endpoint อื่นนอกจาก health ต้องไม่ถูกเปิดสาธารณะ เช่น /actuator/env ต้องไม่คืน 200")
    void nonHealthActuatorEndpointsAreNotPublic() throws Exception {
        mockMvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
    }
}
