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
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
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
                // Public OpenAPI / Swagger documentation endpoints
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/v3/api-docs",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()

                // Public authentication endpoints
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()

                // Authenticated role selection endpoint
                .requestMatchers(HttpMethod.POST, "/api/auth/select-role").authenticated()

                // Authenticated distributor organization selection endpoint
                .requestMatchers(HttpMethod.POST, "/api/auth/select-distributor").authenticated()

                // Public user registration
                .requestMatchers(HttpMethod.POST, "/api/users").permitAll()

                // Public health monitoring
                .requestMatchers(HttpMethod.GET, "/api/health/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/health").permitAll()

                // Public WebSocket handshake endpoint (STOMP frames enforce JWT authentication and ReBAC)
                .requestMatchers("/ws/**", "/ws").permitAll()

                // Role-Based Access Control (RBAC) restrictions
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/distributor/**").hasRole("DISTRIBUTOR")
                .requestMatchers("/api/customer/**").hasRole("CUSTOMER")

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

