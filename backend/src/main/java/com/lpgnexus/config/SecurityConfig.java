package com.lpgnexus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig — Phase 1 (Open/Permissive)
 *
 * Spring Security is on the classpath but authentication is NOT enforced yet.
 * All endpoints are permitted during Phase 1 to allow simple development.
 *
 * TODO (Future Phase): Replace permitAll() with proper JWT / role-based security.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Phase 1: Allow all requests — security will be added in a later phase
                .anyRequest().permitAll()
            );
        return http.build();
    }
}
