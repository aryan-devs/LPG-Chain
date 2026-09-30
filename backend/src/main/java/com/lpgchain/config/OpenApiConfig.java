package com.lpgchain.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenApiConfig — Configures OpenAPI 3.0 / Swagger UI metadata and JWT Bearer security scheme.
 */
@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    @Bean
    public OpenAPI lpgChainOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LPG-Chain API")
                        .description("LPG cylinder booking, distributor management, authentication, authorization and tracking backend")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("LPG-Chain Engineering Team")
                                .email("dev@lpgchain.example"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .addTagsItem(new Tag()
                        .name("WebSocket Real-Time Tracking")
                        .description("STOMP over WebSocket endpoint: /ws | Primary Destination: /topic/deliveries/{deliveryId}/tracking | Authenticate via JWT Bearer in STOMP CONNECT frame"))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT token to authorize requests. Format: Bearer <token>")));
    }
}
