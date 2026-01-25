package com.notificationhub.utils;

import com.notificationhub.entity.User;
import com.notificationhub.enums.Role;
import com.notificationhub.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityUtils Unit Tests")
public class SecurityUtilsTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private SecurityUtils securityUtils;

    private User testUser;
    private UserDetails userDetails;
    private static final String TEST_USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername(TEST_USERNAME);
        testUser.setRole(Role.USER);

        userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(TEST_USERNAME)
                .password("password")
                .authorities("ROLE_USER")
                .build();

        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ========== GET CURRENT USER ==========

    @Test
    @DisplayName("Should return current user when authenticated")
    void shouldReturnCurrentUserWhenAuthenticated() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));

        User result = securityUtils.getCurrentUser();

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(TEST_USERNAME);
        verify(userRepository).findByUsername(TEST_USERNAME);
    }

    @Test
    @DisplayName("Should return null when authentication is null")
    void shouldReturnNullWhenAuthenticationIsNull() {
        when(securityContext.getAuthentication()).thenReturn(null);

        User result = securityUtils.getCurrentUser();

        assertThat(result).isNull();
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    @DisplayName("Should return null when user is not authenticated")
    void shouldReturnNullWhenNotAuthenticated() {
        Authentication authentication = mock(Authentication.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        User result = securityUtils.getCurrentUser();

        assertThat(result).isNull();
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    @DisplayName("Should return null when principal is not UserDetails")
    void shouldReturnNullWhenPrincipalIsNotUserDetails() {
        Authentication authentication = mock(Authentication.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("anonymousUser");

        User result = securityUtils.getCurrentUser();

        assertThat(result).isNull();
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    @DisplayName("Should return null when user not found in database")
    void shouldReturnNullWhenUserNotFoundInDatabase() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

        User result = securityUtils.getCurrentUser();

        assertThat(result).isNull();
        verify(userRepository).findByUsername(TEST_USERNAME);
    }

    // ========== HAS ROLE ==========

    @Test
    @DisplayName("Should return true when user has the specified role")
    void shouldReturnTrueWhenUserHasRole() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);

        boolean result = securityUtils.hasRole("USER");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when user does not have the specified role")
    void shouldReturnFalseWhenUserDoesNotHaveRole() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);

        boolean result = securityUtils.hasRole("ADMIN");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when authentication is null for role check")
    void shouldReturnFalseWhenAuthenticationIsNullForRoleCheck() {
        when(securityContext.getAuthentication()).thenReturn(null);

        boolean result = securityUtils.hasRole("USER");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return true when user has multiple roles including specified one")
    void shouldReturnTrueWhenUserHasMultipleRoles() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, List.of(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("ROLE_ADMIN")
        )
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);

        boolean hasUser = securityUtils.hasRole("USER");
        boolean hasAdmin = securityUtils.hasRole("ADMIN");

        assertThat(hasUser).isTrue();
        assertThat(hasAdmin).isTrue();
    }

    // ========== IS ADMIN ==========

    @Test
    @DisplayName("Should return true when user is admin")
    void shouldReturnTrueWhenUserIsAdmin() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);

        boolean result = securityUtils.isAdmin();

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when user is not admin")
    void shouldReturnFalseWhenUserIsNotAdmin() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);

        boolean result = securityUtils.isAdmin();

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when authentication is null for admin check")
    void shouldReturnFalseWhenAuthenticationIsNullForAdminCheck() {
        when(securityContext.getAuthentication()).thenReturn(null);

        boolean result = securityUtils.isAdmin();

        assertThat(result).isFalse();
    }

    // ========== EDGE CASES ==========

    @Test
    @DisplayName("Should handle empty authorities list")
    void shouldHandleEmptyAuthoritiesList() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, List.of()
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);

        boolean hasRole = securityUtils.hasRole("USER");
        boolean isAdmin = securityUtils.isAdmin();

        assertThat(hasRole).isFalse();
        assertThat(isAdmin).isFalse();
    }

    @Test
    @DisplayName("Should be case sensitive for role names")
    void shouldBeCaseSensitiveForRoleNames() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);

        boolean hasUpperCase = securityUtils.hasRole("USER");
        boolean hasLowerCase = securityUtils.hasRole("user");

        assertThat(hasUpperCase).isTrue();
        assertThat(hasLowerCase).isFalse();
    }
}