package com.notificationhub.util;

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
    private final long EXPIRATION_TIME = 86400000L; // 24 hours

    private UserDetails userDetails;
    private UserDetails adminUserDetails;

    @BeforeEach
    void setUp() {
        // Usar lenient() para evitar UnnecessaryStubbingException
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
    void generateToken_WithUserDetails_ReturnsValidToken() {
        // Act
        String token = jwtUtils.generateToken(userDetails);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());

        // Verify token can be parsed and contains correct claims
        String username = jwtUtils.extractUsername(token);
        assertEquals("testuser", username);

        List<String> roles = jwtUtils.extractRoles(token);
        assertTrue(roles.contains("ROLE_USER"));
    }

    @Test
    @DisplayName("Should include roles in token claims")
    void generateToken_WithUserDetails_IncludesRolesInClaims() {
        // Act
        String token = jwtUtils.generateToken(adminUserDetails);

        // Assert
        List<String> roles = jwtUtils.extractRoles(token);
        assertTrue(roles.contains("ROLE_ADMIN"));
        assertTrue(roles.contains("ROLE_USER"));
        assertEquals(2, roles.size());
    }

    @Test
    @DisplayName("Should extract username from valid token")
    void extractUsername_WithValidToken_ReturnsUsername() {
        // Arrange
        String token = jwtUtils.generateToken(userDetails);

        // Act
        String username = jwtUtils.extractUsername(token);

        // Assert
        assertEquals("testuser", username);
    }

    @Test
    @DisplayName("Should extract expiration date from token")
    void extractExpiration_WithValidToken_ReturnsExpirationDate() {
        // Arrange
        String token = jwtUtils.generateToken(userDetails);

        // Act
        Date expiration = jwtUtils.extractExpiration(token);

        // Assert
        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }

    @Test
    @DisplayName("Should validate token with correct user details")
    void validateToken_WithCorrectUserDetails_ReturnsTrue() {
        // Arrange
        String token = jwtUtils.generateToken(userDetails);

        // Act
        boolean isValid = jwtUtils.validateToken(token, userDetails);

        // Assert
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should invalidate token with incorrect user details")
    void validateToken_WithIncorrectUserDetails_ReturnsFalse() {
        // Arrange
        String token = jwtUtils.generateToken(userDetails);

        UserDetails differentUser = User.builder()
                .username("differentuser")
                .password("pass")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        // Act
        boolean isValid = jwtUtils.validateToken(token, differentUser);

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should validate token without user details")
    void validateToken_WithoutUserDetails_ReturnsTrueForValidToken() {
        // Arrange
        String token = jwtUtils.generateToken(userDetails);

        // Act
        boolean isValid = jwtUtils.validateToken(token);

        // Assert
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should invalidate expired token")
    void validateToken_WithExpiredToken_ReturnsFalse() {
        // Arrange - Create a JwtUtils with very short expiration
        when(jwtProperties.getExpiration()).thenReturn(1L); // 1 ms expiration
        JwtUtils shortLivedJwtUtils = new JwtUtils(jwtProperties);

        String token = shortLivedJwtUtils.generateToken(userDetails);

        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Act
        boolean isValid = shortLivedJwtUtils.validateToken(token);

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should extract roles from token")
    void extractRoles_WithTokenContainingRoles_ReturnsRoleList() {
        // Arrange
        String token = jwtUtils.generateToken(adminUserDetails);

        // Act
        List<String> roles = jwtUtils.extractRoles(token);

        // Assert
        assertNotNull(roles);
        assertEquals(2, roles.size());
        assertTrue(roles.contains("ROLE_ADMIN"));
        assertTrue(roles.contains("ROLE_USER"));
    }

    @Test
    @DisplayName("Should return empty list for token without roles")
    void extractRoles_WithTokenWithoutRoles_ReturnsEmptyList() {
        // Arrange - Create user without authorities
        UserDetails userWithoutRoles = User.builder()
                .username("noroles")
                .password("pass")
                .authorities(Collections.emptyList())
                .build();

        String token = jwtUtils.generateToken(userWithoutRoles);

        // Act
        List<String> roles = jwtUtils.extractRoles(token);

        // Assert
        assertNotNull(roles);
        assertTrue(roles.isEmpty());
    }

    @Test
    @DisplayName("Should return expiration time from properties")
    void getExpirationTime_ReturnsValueFromProperties() {
        // Act
        Long expirationTime = jwtUtils.getExpirationTime();

        // Assert
        assertEquals(EXPIRATION_TIME, expirationTime);
        verify(jwtProperties).getExpiration();
    }

    @Test
    @DisplayName("Should handle malformed token gracefully")
    void validateToken_WithMalformedToken_ReturnsFalse() {
        // Arrange
        String malformedToken = "malformed.token.here";

        // Act
        boolean isValid = jwtUtils.validateToken(malformedToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should handle token with wrong signature")
    void validateToken_WithWrongSignature_ReturnsFalse() {
        // Arrange - Create token with different secret
        JwtProperties differentProperties = mock(JwtProperties.class);
        when(differentProperties.getSecret()).thenReturn("differentSecretKeyForTesting123!");
        when(differentProperties.getExpiration()).thenReturn(EXPIRATION_TIME);

        JwtUtils differentJwtUtils = new JwtUtils(differentProperties);
        String tokenWithDifferentSecret = differentJwtUtils.generateToken(userDetails);

        // Act - Try to validate with original JwtUtils
        boolean isValid = jwtUtils.validateToken(tokenWithDifferentSecret);

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should handle null token gracefully")
    void validateToken_WithNullToken_ReturnsFalse() {
        // Act
        boolean isValid = jwtUtils.validateToken(null);

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should handle empty token gracefully")
    void validateToken_WithEmptyToken_ReturnsFalse() {
        // Act
        boolean isValid = jwtUtils.validateToken("");

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should generate different tokens for same user at different times")
    void generateToken_ForSameUserAtDifferentTimes_GeneratesDifferentTokens() {
        // Act
        String token1 = jwtUtils.generateToken(userDetails);

        // Small delay to ensure different issuedAt time
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String token2 = jwtUtils.generateToken(userDetails);

        // Assert
        assertNotEquals(token1, token2);

        // But usernames should be the same
        assertEquals(
                jwtUtils.extractUsername(token1),
                jwtUtils.extractUsername(token2)
        );
    }
}
