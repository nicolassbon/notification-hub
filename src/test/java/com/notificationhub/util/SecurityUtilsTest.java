package com.notificationhub.util;

import com.notificationhub.entity.User;
import com.notificationhub.enums.Role;
import com.notificationhub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityUtils Unit Tests")
public class SecurityUtilsTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SecurityUtils securityUtils;

    private User testUser;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRole(Role.USER);

        userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("testuser")
                .password("password")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
    }

    @Test
    @DisplayName("Should return current user when authenticated")
    void getCurrentUserWhenAuthenticatedReturnsUser() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        User result = securityUtils.getCurrentUser();

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should return null when not authenticated")
    void getCurrentUserWhenNotAuthenticatedReturnsNull() {
        when(securityContext.getAuthentication()).thenReturn(null);

        User result = securityUtils.getCurrentUser();

        assertNull(result);
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    @DisplayName("Should return null when authentication principal is not UserDetails")
    void getCurrentUserWhenPrincipalNotUserDetailsReturnsNull() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("not-user-details");

        User result = securityUtils.getCurrentUser();

        assertNull(result);
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    @DisplayName("Should return null when user not found in repository")
    void getCurrentUserWhenUserNotFoundReturnsNull() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        User result = securityUtils.getCurrentUser();

        assertNull(result);
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should return current username when authenticated")
    void getCurrentUsernameWhenAuthenticatedReturnsUsername() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        String result = securityUtils.getCurrentUsername();

        assertEquals("testuser", result);
    }

    @Test
    @DisplayName("Should return null when not authenticated for username")
    void getCurrentUsernameWhenNotAuthenticatedReturnsNull() {
        when(securityContext.getAuthentication()).thenReturn(null);

        String result = securityUtils.getCurrentUsername();

        assertNull(result);
    }

    @Test
    @DisplayName("Should return null when principal is not UserDetails for username")
    void getCurrentUsernameWhenPrincipalNotUserDetailsReturnsNull() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("not-user-details");

        String result = securityUtils.getCurrentUsername();

        assertNull(result);
    }

    @Test
    @DisplayName("Should return true when user has specific role")
    void hasRoleWhenUserHasRoleReturnsTrue() {
        Collection<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);

        boolean result = securityUtils.hasRole("ADMIN");

        assertTrue(result);
    }

    @Test
    @DisplayName("Should return false when user does not have specific role")
    void hasRoleWhenUserDoesNotHaveRoleReturnsFalse() {
        Collection<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);

        boolean result = securityUtils.hasRole("ADMIN");

        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false when no authentication for role check")
    void hasRoleWhenNoAuthenticationReturnsFalse() {
        when(securityContext.getAuthentication()).thenReturn(null);

        boolean result = securityUtils.hasRole("ADMIN");

        assertFalse(result);
    }

    @Test
    @DisplayName("Should return true when user is admin")
    void isAdminWhenUserIsAdminReturnsTrue() {
        Collection<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);

        boolean result = securityUtils.isAdmin();

        assertTrue(result);
    }

    @Test
    @DisplayName("Should return false when user is not admin")
    void isAdminWhenUserIsNotAdminReturnsFalse() {
        Collection<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);

        boolean result = securityUtils.isAdmin();

        assertFalse(result);
    }

    @Test
    @DisplayName("Should handle multiple authorities correctly")
    void hasRoleWhenMultipleAuthoritiesChecksCorrectly() {
        Collection<GrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("ROLE_ADMIN")
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);

        assertTrue(securityUtils.hasRole("USER"));
        assertTrue(securityUtils.hasRole("ADMIN"));
        assertFalse(securityUtils.hasRole("SUPER_ADMIN"));
    }
}
