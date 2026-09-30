package com.lpgchain.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * OpenApiDocumentationIntegrationTest
 *
 * Verifies that:
 * 1. OpenAPI JSON specification (/v3/api-docs) is publicly accessible and contains the correct metadata and JWT Bearer scheme.
 * 2. Swagger UI (/swagger-ui/index.html, /swagger-ui.html) is publicly accessible without authentication.
 * 3. Business endpoints (/api/bookings, /api/admin/test) remain strictly protected (401 Unauthorized without JWT).
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocumentationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("OPENAPI-01: OpenAPI 3.0 JSON specification endpoint (/v3/api-docs) is publicly accessible with correct metadata")
    void shouldServeOpenApiJsonSpecification() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("LPG-Chain API"))
                .andExpect(jsonPath("$.info.version").value("1.0.0"))
                .andExpect(jsonPath("$.info.description").value(
                        "LPG cylinder booking, distributor management, authentication, authorization and tracking backend"))
                // Verify JWT Bearer security scheme is registered in OpenAPI components
                .andExpect(jsonPath("$.components.securitySchemes['Bearer Authentication'].type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes['Bearer Authentication'].scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes['Bearer Authentication'].bearerFormat").value("JWT"))
                // Verify important API endpoints are documented
                .andExpect(jsonPath("$.paths['/api/auth/login']").exists())
                .andExpect(jsonPath("$.paths['/api/bookings']").exists())
                .andExpect(jsonPath("$.paths['/api/health']").exists())
                .andExpect(jsonPath("$.paths['/api/users']").exists())
                .andExpect(jsonPath("$.paths['/api/distributors']").exists())
                .andExpect(jsonPath("$.paths['/api/memberships']").exists())
                .andExpect(jsonPath("$.paths['/api/deliveries']").exists())
                .andExpect(jsonPath("$.paths['/api/drivers']").exists())
                .andExpect(jsonPath("$.paths['/api/vehicles']").exists());
    }

    @Test
    @DisplayName("OPENAPI-02: Swagger UI endpoint (/swagger-ui/index.html) is publicly accessible without authentication")
    void shouldServeSwaggerUiHtml() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }

    @Test
    @DisplayName("OPENAPI-03: Swagger UI redirect (/swagger-ui.html) is publicly accessible and redirects to Swagger UI index")
    void shouldRedirectFromSwaggerUiHtml() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("OPENAPI-04: Business APIs remain strictly protected when accessed without credentials")
    void shouldKeepBusinessApisProtectedWhileSwaggerIsPublic() throws Exception {
        // Bookings endpoint must require authentication (401)
        mockMvc.perform(get("/api/bookings"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Unauthorized"));

        // Admin endpoint must require authentication (401)
        mockMvc.perform(get("/api/admin/test"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        // Distributor endpoint must require authentication (401)
        mockMvc.perform(get("/api/distributor/test"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("OPENAPI-05: OpenAPI specification never exposes sensitive configuration or secrets")
    void shouldNeverExposeSecretsInOpenApiSpecification() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("test-environment-pepper-secret-key"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("TestJwtSecretKeyForAutomatedSecurityTesting"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("DB_PASSWORD"))));
    }
}
