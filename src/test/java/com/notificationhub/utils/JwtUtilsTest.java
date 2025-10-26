package com.notificationhub.utils;

import com.notificationhub.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtUtilsTest {

    @Mock
    private JwtProperties jwtProperties;

    private JwtUtils jwtUtils;

    private final String SECRET_KEY = "mySuperSecretKeyForTestingPurposesOnly123!";
    private final long EXPIRATION_TIME = 86400000L;

    private UserDetails userDetails;
    private UserDetails adminUserDetails;

    @BeforeEach
    void setUp() {
        lenient().when(jwtProperties.getSecret()).thenReturn(SECRET_KEY);
        lenient().when(jwtProperties.getExpiration()).thenReturn(EXPIRATION_TIME);

        jwtUtils = new JwtUtils(jwtProperties);

        userDetails = User.builder()
                .username("testuser")
                .password("password")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        adminUserDetails = User.builder()
                .username("admin")
                .password("adminpass")
                .authorities(Arrays.asList(
                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_USER")
                ))
                .build();
    }

    @Test
    @DisplayName("Should generate valid token for user details")
    void generateTokenWithUserDetailsReturnsValidToken() {
        String token = jwtUtils.generateToken(userDetails);

        assertNotNull(token);
        assertFalse(token.isEmpty());

        String username = jwtUtils.extractUsername(token);
        assertEquals("testuser", username);

        List<String> roles = jwtUtils.extractRoles(token);
        assertTrue(roles.contains("ROLE_USER"));
    }

    @Test
    @DisplayName("Should include roles in token claims")
    void generateTokenWithUserDetailsIncludesRolesInClaims() {
        String token = jwtUtils.generateToken(adminUserDetails);

        List<String> roles = jwtUtils.extractRoles(token);
        assertTrue(roles.contains("ROLE_ADMIN"));
        assertTrue(roles.contains("ROLE_USER"));
        assertEquals(2, roles.size());
    }

    @Test
    @DisplayName("Should extract username from valid token")
    void extractUsernameWithValidTokenReturnsUsername() {
        String token = jwtUtils.generateToken(userDetails);

        String username = jwtUtils.extractUsername(token);

        assertEquals("testuser", username);
    }

    @Test
    @DisplayName("Should extract expiration date from token")
    void extractExpirationWithValidTokenReturnsExpirationDate() {
        String token = jwtUtils.generateToken(userDetails);

        Date expiration = jwtUtils.extractExpiration(token);

        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }

    @Test
    @DisplayName("Should validate token with correct user details")
    void validateTokenWithCorrectUserDetailsReturnsTrue() {
        String token = jwtUtils.generateToken(userDetails);

        boolean isValid = jwtUtils.validateToken(token, userDetails);

        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should invalidate token with incorrect user details")
    void validateTokenWithIncorrectUserDetailsReturnsFalse() {
        String token = jwtUtils.generateToken(userDetails);

        UserDetails differentUser = User.builder()
                .username("differentuser")
                .password("pass")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        boolean isValid = jwtUtils.validateToken(token, differentUser);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should validate token without user details")
    void validateTokenWithoutUserDetailsReturnsTrueForValidToken() {
        String token = jwtUtils.generateToken(userDetails);

        boolean isValid = jwtUtils.validateToken(token);

        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should invalidate expired token")
    void validateTokenWithExpiredTokenReturnsFalse() {
        when(jwtProperties.getExpiration()).thenReturn(1L);
        JwtUtils shortLivedJwtUtils = new JwtUtils(jwtProperties);

        String token = shortLivedJwtUtils.generateToken(userDetails);

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        boolean isValid = shortLivedJwtUtils.validateToken(token);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should extract roles from token")
    void extractRolesWithTokenContainingRolesReturnsRoleList() {
        String token = jwtUtils.generateToken(adminUserDetails);

        List<String> roles = jwtUtils.extractRoles(token);

        assertNotNull(roles);
        assertEquals(2, roles.size());
        assertTrue(roles.contains("ROLE_ADMIN"));
        assertTrue(roles.contains("ROLE_USER"));
    }

    @Test
    @DisplayName("Should return empty list for token without roles")
    void extractRolesWithTokenWithoutRolesReturnsEmptyList() {
        UserDetails userWithoutRoles = User.builder()
                .username("noroles")
                .password("pass")
                .authorities(Collections.emptyList())
                .build();

        String token = jwtUtils.generateToken(userWithoutRoles);

        List<String> roles = jwtUtils.extractRoles(token);

        assertNotNull(roles);
        assertTrue(roles.isEmpty());
    }

    @Test
    @DisplayName("Should return expiration time from properties")
    void getExpirationTimeReturnsValueFromProperties() {
        Long expirationTime = jwtUtils.getExpirationTime();

        assertEquals(EXPIRATION_TIME, expirationTime);
        verify(jwtProperties).getExpiration();
    }

    @Test
    @DisplayName("Should handle malformed token gracefully")
    void validateTokenWithMalformedTokenReturnsFalse() {
        String malformedToken = "malformed.token.here";

        boolean isValid = jwtUtils.validateToken(malformedToken);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should handle token with wrong signature")
    void validateToken_WithWrongSignature_ReturnsFalse() {
        JwtProperties differentProperties = mock(JwtProperties.class);
        when(differentProperties.getSecret()).thenReturn("differentSecretKeyForTesting123!");
        when(differentProperties.getExpiration()).thenReturn(EXPIRATION_TIME);

        JwtUtils differentJwtUtils = new JwtUtils(differentProperties);
        String tokenWithDifferentSecret = differentJwtUtils.generateToken(userDetails);

        boolean isValid = jwtUtils.validateToken(tokenWithDifferentSecret);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should handle null token gracefully")
    void validateTokenWithNullTokenReturnsFalse() {
        boolean isValid = jwtUtils.validateToken(null);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should handle empty token gracefully")
    void validateTokenWithEmptyTokenReturnsFalse() {
        boolean isValid = jwtUtils.validateToken("");

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should generate different tokens for same user at different times")
    void generateTokenForSameUserAtDifferentTimesGeneratesDifferentTokens() {
        String token1 = jwtUtils.generateToken(userDetails);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String token2 = jwtUtils.generateToken(userDetails);

        assertNotEquals(token1, token2);

        assertEquals(
                jwtUtils.extractUsername(token1),
                jwtUtils.extractUsername(token2)
        );
    }
}
