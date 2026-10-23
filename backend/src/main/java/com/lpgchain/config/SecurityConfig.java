package com.lpgchain.config;

import com.lpgchain.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.security.config.Customizer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * SecurityConfig — Configures JWT-based stateless authentication and RBAC.
 *
 * Public endpoints:
 *  - POST /api/auth/login (and /api/auth/**)
 *  - POST /api/users (user registration)
 *  - GET /api/health/** (health check)
 *
 * Role-restricted route groups:
 *  - /api/admin/**       -> ADMIN
 *  - /api/distributor/** -> DISTRIBUTOR
 *  - /api/customer/**    -> CUSTOMER
 *
 * All other endpoints require authentication.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * Registers BCryptPasswordEncoder as the application-wide PasswordEncoder.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("http://localhost:[*]", "http://127.0.0.1:[*]", "http://localhost:5173", "http://localhost:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"status\":401,\"message\":\"Unauthorized\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"status\":403,\"message\":\"Forbidden: Access denied\"}");
                })
            )
            .authorizeHttpRequests(auth -> auth
                // Allow all CORS preflight requests
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Public OpenAPI / Swagger documentation endpoints
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/v3/api-docs",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()

                // Public authentication and customer registration endpoints
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register", "/api/auth/google").permitAll()

                // Authenticated role selection endpoint
                .requestMatchers(HttpMethod.POST, "/api/auth/select-role").authenticated()

                // Authenticated distributor organization selection endpoint
                .requestMatchers(HttpMethod.POST, "/api/auth/select-distributor").authenticated()

                // Public user registration
                .requestMatchers(HttpMethod.POST, "/api/users").permitAll()

                // User administration: listing registered users and account deletion restricted to ADMIN
                .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")

                // Public health monitoring
                .requestMatchers(HttpMethod.GET, "/api/health/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/health").permitAll()

                // Public WebSocket handshake endpoint (STOMP frames enforce JWT authentication and ReBAC)
                .requestMatchers("/ws/**", "/ws").permitAll()

                // Public distributor directory search (sanitized DTOs, anonymous access permitted)
                .requestMatchers(HttpMethod.GET, "/api/distributors/public/**").permitAll()

                // Public cylinder allocation & sales transparency figures (anonymous and customer read-only)
                .requestMatchers(HttpMethod.GET, "/api/transparency/**").permitAll()

                // Public AI Operations Assistant (answers general FAQs anonymously; prompts login for account-specific queries)
                .requestMatchers("/api/ai/**").permitAll()

                // Cylinder Allocation management: official quota creation, updates, and adjustments restricted to ADMIN
                .requestMatchers(HttpMethod.POST, "/api/allocations", "/api/allocations/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/allocations/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/allocations/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/allocations/**").hasAnyRole("ADMIN", "DISTRIBUTOR")
                .requestMatchers("/api/allocations/**").authenticated()

                // Role-Based Access Control (RBAC) restrictions
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/distributor/**").hasRole("DISTRIBUTOR")
                .requestMatchers("/api/inventory/**").hasAnyRole("ADMIN", "DISTRIBUTOR")
                .requestMatchers("/api/customer/**").hasRole("CUSTOMER")
                .requestMatchers("/api/payments/**").authenticated()
                .requestMatchers("/api/complaints/**").authenticated()
                .requestMatchers("/api/documents/**").authenticated()


                // Distributor administration: agency creation, status updates, and deletion restricted to ADMIN
                .requestMatchers(HttpMethod.POST, "/api/distributors", "/api/distributors/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/distributors/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/distributors/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/distributors/**").hasRole("ADMIN")

                // Driver and Vehicle management: restricted to ADMIN and DISTRIBUTOR
                .requestMatchers("/api/drivers/**").hasAnyRole("ADMIN", "DISTRIBUTOR")
                .requestMatchers("/api/vehicles/**").hasAnyRole("ADMIN", "DISTRIBUTOR")

                // Delivery operations require authentication (fine-grained ReBAC enforced in service layer)
                .requestMatchers("/api/deliveries/**").authenticated()

                // GPS Tracking operations require authentication (fine-grained ReBAC enforced in service layer)
                .requestMatchers("/api/tracking/**").authenticated()

                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

