package com.lpgchain.security;

import com.lpgchain.entity.Role;
import com.lpgchain.entity.User;
import com.lpgchain.entity.UserRole;
import com.lpgchain.repository.UserRepository;
import com.lpgchain.repository.UserRoleRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * JwtAuthenticationFilter — Intercepts HTTP requests and validates JWT Bearer tokens.
 *
 * If a valid JWT token is found, extracts the user email, loads the user and their
 * assigned roles from UserRoleRepository, converts them into GrantedAuthority values,
 * and sets authentication in the Spring SecurityContextHolder.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   UserRepository userRepository,
                                   UserRoleRepository userRoleRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            String email = jwtTokenProvider.getEmailFromToken(token);

            if (StringUtils.hasText(email)) {
                Optional<User> userOptional = userRepository.findByEmail(email);

                if (userOptional.isPresent()) {
                    User user = userOptional.get();

                    String activeRoleClaim = jwtTokenProvider.getRoleFromToken(token);
                    List<UserRole> userRoles = userRoleRepository.findByUser(user);

                    List<GrantedAuthority> authorities;

                    if (StringUtils.hasText(activeRoleClaim)) {
                        String normalizedActiveRole = normalizeRole(activeRoleClaim);

                        // Verify that the active role in JWT still belongs to the user in database
                        boolean roleStillAssigned = userRoles.stream()
                                .map(UserRole::getRole)
                                .filter(Objects::nonNull)
                                .map(Role::getName)
                                .filter(StringUtils::hasText)
                                .map(this::normalizeRole)
                                .anyMatch(normalizedActiveRole::equals);

                        if (roleStillAssigned) {
                            authorities = List.of(new SimpleGrantedAuthority(normalizedActiveRole));
                        } else {
                            // Active role claim is invalid or was revoked in database
                            authorities = Collections.emptyList();
                        }
                    } else {
                        // No active role claim in token (pre-auth token or legacy single-role token)
                        List<String> assignedNormalizedRoles = userRoles.stream()
                                .map(UserRole::getRole)
                                .filter(Objects::nonNull)
                                .map(Role::getName)
                                .filter(StringUtils::hasText)
                                .map(this::normalizeRole)
                                .distinct()
                                .toList();

                        // If user has exactly one role in database, grant it for backward compatibility
                        if (assignedNormalizedRoles.size() == 1) {
                            authorities = List.of(new SimpleGrantedAuthority(assignedNormalizedRoles.get(0)));
                        } else {
                            // Multi-role user with no active role selected yet -> empty authorities
                            authorities = Collections.emptyList();
                        }
                    }

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user.getEmail(), null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Normalizes a role name to Spring Security convention:
     * - Trim whitespace
     * - Convert to uppercase
     * - Ensure single "ROLE_" prefix (prevents "ROLE_ROLE_...")
     */
    private String normalizeRole(String roleName) {
        String trimmed = roleName.trim().toUpperCase();
        if (trimmed.startsWith("ROLE_")) {
            return trimmed;
        }
        return "ROLE_" + trimmed;
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}

