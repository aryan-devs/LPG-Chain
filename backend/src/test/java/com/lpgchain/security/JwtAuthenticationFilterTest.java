package com.lpgchain.security;

import com.lpgchain.entity.Role;
import com.lpgchain.entity.User;
import com.lpgchain.entity.UserRole;
import com.lpgchain.repository.UserRepository;
import com.lpgchain.repository.UserRoleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    private JwtTokenProvider jwtTokenProvider;
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private User testUser;
    private Role customerRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        jwtTokenProvider = new JwtTokenProvider(
                "TestJwtSecretKeyForAutomatedSecurityTestingMustBeAtLeast256BitsLongForHmacSha256!",
                3600000
        );
        jwtAuthenticationFilter = new JwtAuthenticationFilter(
                jwtTokenProvider,
                userRepository,
                userRoleRepository
        );

        customerRole = new Role(1L, "CUSTOMER");
        adminRole = new Role(2L, "ADMIN");

        testUser = new User(1L, "Test User", "test@example.com", "hash");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // =========================================================================
    // TEST 1: Request without Authorization header
    // =========================================================================
    @Test
    @DisplayName("FILTER 1: Request without Authorization header remains unauthenticated and proceeds through chain")
    void shouldRemainUnauthenticatedWhenAuthorizationHeaderMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    // =========================================================================
    // TEST 2: Authorization header: Authorization: Bearer <valid-token>
    // =========================================================================
    @Test
    @DisplayName("FILTER 2: Valid Bearer JWT establishes authentication with verified database authorities")
    void shouldAuthenticateRequestWithValidBearerToken() throws Exception {
        String token = jwtTokenProvider.generateToken(1L, "test@example.com", "CUSTOMER");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRoleRepository.findByUser(testUser)).thenReturn(List.of(
                new UserRole(1L, testUser, customerRole)
        ));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.isAuthenticated());
        assertEquals("test@example.com", auth.getName());
        assertEquals(1, auth.getAuthorities().size());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER")));
    }

    // =========================================================================
    // TEST 3: Invalid JWT
    // =========================================================================
    @Test
    @DisplayName("FILTER 3: Invalid JWT does not establish authentication")
    void shouldNotAuthenticateWhenTokenIsInvalid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid.token.payload");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    // =========================================================================
    // TEST 4: Malformed Authorization header
    // =========================================================================
    @Test
    @DisplayName("FILTER 4: Malformed Authorization header is handled safely without throwing exceptions")
    void shouldHandleMalformedAuthorizationHeadersSafely() throws Exception {
        String[] malformedHeaders = {
                "Basic dXNlcjpwYXNz",
                "Bearer",
                "Bearer ",
                "Token abc123",
                "   "
        };

        for (String header : malformedHeaders) {
            SecurityContextHolder.clearContext();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", header);
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain filterChain = new MockFilterChain();

            jwtAuthenticationFilter.doFilter(request, response, filterChain);

            assertNull(SecurityContextHolder.getContext().getAuthentication(),
                    "Expected null authentication for header: " + header);
        }
    }

    // =========================================================================
    // TEST 5: Valid JWT for a user that no longer has the active role (revocation check)
    // =========================================================================
    @Test
    @DisplayName("FILTER 5: Valid JWT with active role revoked in database grants no authorities (prevents unauthorized access)")
    void shouldNotGrantAuthoritiesIfRoleWasRevokedInDatabase() throws Exception {
        // Generate real valid token claiming role ADMIN
        String tokenWithRevokedRole = jwtTokenProvider.generateToken(1L, "test@example.com", "ADMIN");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + tokenWithRevokedRole);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        // Database only has CUSTOMER role (ADMIN role was revoked or never assigned)
        when(userRoleRepository.findByUser(testUser)).thenReturn(List.of(
                new UserRole(1L, testUser, customerRole)
        ));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        // Authorities must be empty because the claimed role is not found in database
        assertTrue(auth.getAuthorities().isEmpty(), "Authorities must be empty when claimed active role is not in DB");
    }

    // =========================================================================
    // TEST 6: Valid JWT for user that does not exist in database
    // =========================================================================
    @Test
    @DisplayName("FILTER 6: Valid JWT for non-existent database user does not establish authentication")
    void shouldNotAuthenticateIfUserDoesNotExistInDatabase() throws Exception {
        String tokenForDeletedUser = jwtTokenProvider.generateToken(99L, "deleted@example.com", "CUSTOMER");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + tokenForDeletedUser);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        when(userRepository.findByEmail("deleted@example.com")).thenReturn(Optional.empty());

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
