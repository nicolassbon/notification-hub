package com.notificationhub.security.filter;

import com.notificationhub.security.service.CustomUserDetailsService;
import com.notificationhub.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthFilter Unit Tests")
public class JwtAuthFilterTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should authenticate user when JWT is valid")
    void doFilterValidTokenAuthenticatesUser() throws ServletException, IOException {
        String token = "valid.jwt.token";
        String username = "testuser";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtils.validateToken(token)).thenReturn(true);
        when(jwtUtils.extractUsername(token)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());

        jwtAuthFilter.doFilter(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(userDetails, SecurityContextHolder.getContext().getAuthentication().getPrincipal());

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should set error attribute when token is invalid")
    void doFilterInvalidTokenSetsErrorAttribute() throws ServletException, IOException {
        String token = "invalid.jwt.token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtils.validateToken(token)).thenReturn(false);

        jwtAuthFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());


        verify(request).setAttribute("auth.error", "INVALID_TOKEN");

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should proceed without authentication when no token provided")
    void doFilterNoTokenProceedsWithoutAuth() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtUtils, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should proceed without authentication when header format is wrong")
    void doFilterWrongHeaderFormatProceedsWithoutAuth() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Basic 12345");

        jwtAuthFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtUtils, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should set error attribute when user not found")
    void doFilterUserNotFoundSetsErrorAttribute() throws ServletException, IOException {
        String token = "valid.token.unknown.user";
        String username = "unknown";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtils.validateToken(token)).thenReturn(true);
        when(jwtUtils.extractUsername(token)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username))
                .thenThrow(new UsernameNotFoundException("User not found"));

        jwtAuthFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(request).setAttribute("auth.error", "USER_NOT_FOUND");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should set error attribute on generic exception")
    void doFilterGenericExceptionSetsErrorAttribute() throws ServletException, IOException {
        String token = "error.token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtils.validateToken(token)).thenThrow(new RuntimeException("Unexpected error"));

        jwtAuthFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(request).setAttribute("auth.error", "AUTH_FAILED");
        verify(filterChain).doFilter(request, response);
    }
}
