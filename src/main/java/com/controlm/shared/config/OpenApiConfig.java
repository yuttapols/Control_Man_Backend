package com.controlm.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI document for both API surfaces.
 *
 * <p>The Portal API uses a JWT bearer token and the Holiday Web Service uses an API key
 * header. Both schemes are declared here; each operation opts into the one it accepts.
 */
@Configuration
public class OpenApiConfig {

    public static final String PORTAL_JWT = "portalJwt";
    public static final String CONSUMER_API_KEY = "consumerApiKey";

    @Value("${app.environment:LOCAL}")
    private String environment;

    @Bean
    OpenAPI controlMOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Thai Holiday Control API")
                        .version("v1")
                        .description("Portal API and published Holiday Web Service. Environment: " + environment))
                .components(new Components()
                        .addSecuritySchemes(
                                PORTAL_JWT,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Short-lived Portal access token"))
                        .addSecuritySchemes(
                                CONSUMER_API_KEY,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("X-API-Key")
                                        .description("API consumer credential, scoped to one environment")));
    }
}
