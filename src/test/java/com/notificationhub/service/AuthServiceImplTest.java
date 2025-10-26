package com.notificationhub.service;

import com.notificationhub.dto.response.AuthResponse;
import com.notificationhub.dto.request.LoginRequest;
import com.notificationhub.dto.request.RegisterRequest;
import com.notificationhub.dto.response.RegisterResponse;
import com.notificationhub.entity.User;
import com.notificationhub.enums.Role;
import com.notificationhub.exception.InvalidCredentialsException;
import com.notificationhub.repository.UserRepository;
import com.notificationhub.util.JwtUtils;
import com.notificationhub.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Unit Tests")
public class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private SecurityUtils securityUtils;

    private AuthServiceImpl authService;

    private User testUser;
    private UserDetails userDetails;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository, passwordEncoder, jwtUtils, authenticationManager, securityUtils
        );

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .passwordHash("$2a$10$encodedPassword")
                .role(Role.USER)
                .dailyMessageLimit(100)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("testuser")
                .password("$2a$10$encodedPassword")
                .authorities("ROLE_USER")
                .build();

        registerRequest = new RegisterRequest("testuser", "password123");
        loginRequest = new LoginRequest("testuser", "password123");
    }

    @Test
    @DisplayName("Should register new user successfully without JWT token")
    void registerNewUserReturnsRegisterResponse() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        RegisterResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("User registered successfully", response.getMessage());
        assertEquals("testuser", response.getUsername());
        assertNotNull(response.getTimestamp());

        verify(jwtUtils, never()).generateToken(any(UserDetails.class));
        verify(jwtUtils, never()).getExpirationTime();

        verify(userRepository).findByUsername("testuser");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when username already exists")
    void registerExistingUsernameThrowsException() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(registerRequest)
        );

        assertEquals("Username already exists", exception.getMessage());
        verify(userRepository).findByUsername("testuser");
        verify(userRepository, never()).save(any(User.class));
        verify(jwtUtils, never()).generateToken(any(UserDetails.class));
    }

    @Test
    @DisplayName("Should login user successfully with valid credentials and return JWT token")
    void loginValidCredentialsReturnsAuthResponse() {
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(jwtUtils.generateToken(userDetails)).thenReturn("jwt-token");
        when(jwtUtils.getExpirationTime()).thenReturn(86400000L);

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("testuser", response.getUsername());
        assertEquals("USER", response.getRole());
        assertEquals(86400000L, response.getExpiresIn());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByUsername("testuser");
        verify(jwtUtils).generateToken(userDetails);
    }

    @Test
    @DisplayName("Should throw exception when authentication fails")
    void loginInvalidCredentialsThrowsException() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(loginRequest)
        );

        assertEquals("Invalid username or password", exception.getMessage());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    @DisplayName("Should throw exception when user not found after authentication")
    void loginUserNotFoundAfterAuthThrowsException() {
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(loginRequest)
        );

        assertEquals("Invalid username or password", exception.getMessage());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should return current user when authenticated")
    void getCurrentUserWhenAuthenticatedReturnsUser() {
        when(securityUtils.getCurrentUser()).thenReturn(testUser);

        User currentUser = authService.getCurrentUser();

        assertNotNull(currentUser);
        assertEquals("testuser", currentUser.getUsername());
        verify(securityUtils).getCurrentUser();
    }

    @Test
    @DisplayName("Should throw exception when no authenticated user")
    void getCurrentUserWhenNotAuthenticatedThrowsException() {
        when(securityUtils.getCurrentUser()).thenReturn(null);

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.getCurrentUser()
        );

        assertEquals("Invalid credentials", exception.getMessage());
        verify(securityUtils).getCurrentUser();
    }

    @Test
    @DisplayName("Should return true when current user is admin")
    void isCurrentUserAdminWhenUserIsAdminReturnsTrue() {
        when(securityUtils.isAdmin()).thenReturn(true);

        boolean isAdmin = authService.isCurrentUserAdmin();

        assertTrue(isAdmin);
        verify(securityUtils).isAdmin();
    }

    @Test
    @DisplayName("Should return false when current user is not admin")
    void isCurrentUserAdminWhenUserIsNotAdminReturnsFalse() {
        when(securityUtils.isAdmin()).thenReturn(false);

        boolean isAdmin = authService.isCurrentUserAdmin();

        assertFalse(isAdmin);
        verify(securityUtils).isAdmin();
    }

    @Test
    @DisplayName("Should encode password during registration")
    void registerPasswordIsEncoded() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        RegisterResponse response = authService.register(registerRequest);

        assertNotNull(response);
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(argThat(user ->
                user.getPasswordHash().equals("$2a$10$encodedPassword")
        ));
        verify(jwtUtils, never()).generateToken(any(UserDetails.class));
    }

    @Test
    @DisplayName("Should set default role and message limit during registration")
    void registerSetsDefaultValues() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        RegisterResponse response = authService.register(registerRequest);

        assertNotNull(response);
        verify(userRepository).save(argThat(user ->
                user.getRole() == Role.USER &&
                        user.getDailyMessageLimit() == 100
        ));
        verify(jwtUtils, never()).generateToken(any(UserDetails.class));
    }

    @Test
    @DisplayName("Should handle authentication exception during login")
    void loginAuthenticationExceptionThrowsInvalidCredentials() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new RuntimeException("Authentication service unavailable"));

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(loginRequest)
        );

        assertEquals("Invalid username or password", exception.getMessage());
    }

    @Test
    @DisplayName("Should NOT create UserDetails for JWT during registration")
    void registerDoesNotCreateUserDetailsForJWT() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        RegisterResponse response = authService.register(registerRequest);

        assertNotNull(response);
        verify(jwtUtils, never()).generateToken(any(UserDetails.class));
    }

    @Test
    @DisplayName("Should handle null username in register request")
    void registerNullUsernameThrowsException() {
        RegisterRequest nullUsernameRequest = new RegisterRequest(null, "password");

        assertThrows(Exception.class, () -> authService.register(nullUsernameRequest));
    }

    @Test
    @DisplayName("Should handle null password in register request")
    void registerNullPasswordThrowsException() {
        RegisterRequest nullPasswordRequest = new RegisterRequest("user", null);

        assertThrows(Exception.class, () -> authService.register(nullPasswordRequest));
    }

}
