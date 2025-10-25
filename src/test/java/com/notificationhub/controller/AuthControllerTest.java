package com.notificationhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationhub.dto.response.AuthResponse;
import com.notificationhub.dto.request.LoginRequest;
import com.notificationhub.dto.request.RegisterRequest;
import com.notificationhub.exception.GlobalExceptionHandler;
import com.notificationhub.service.IAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Unit Tests")
public class AuthControllerTest {

    private final String API_REGISTER = "/api/auth/register";
    private final String API_LOGIN = "/api/auth/login";

    private MockMvc mockMvc;

    @Mock
    private IAuthService authService;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper;

    private RegisterRequest validRegisterRequest;
    private LoginRequest validLoginRequest;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        validRegisterRequest = new RegisterRequest("testuser", "password123");
        validLoginRequest = new LoginRequest("testuser", "password123");

        authResponse = AuthResponse.builder()
                .token("jwt-token")
                .expiresIn(86400000L)
                .username("testuser")
                .role("USER")
                .build();
    }

    @Test
    @DisplayName("Should use correct content type in responses")
    void registerResponseHasCorrectContentType() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post(API_REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegisterRequest)))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Should register user successfully and return 201 CREATED")
    void registerValidRequestReturnsCreated() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post(API_REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegisterRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.expiresIn").value(86400000L));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Should login user successfully and return 200 OK")
    void loginValidRequestReturnsOk() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post(API_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.expiresIn").value(86400000L));

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("Should return 400 for register request with empty username")
    void registerEmptyUsernameReturnsBadRequest() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest("", "password123");

        mockMvc.perform(post(API_REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Should return 400 for register request with null username")
    void registerNullUsernameReturnsBadRequest() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest(null, "password123");

        mockMvc.perform(post(API_REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Should return 400 for register request with short password")
    void registerShortPasswordReturnsBadRequest() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest("testuser", "123");

        mockMvc.perform(post(API_REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Should return 400 for login request with empty username")
    void loginEmptyUsernameReturnsBadRequest() throws Exception {
        LoginRequest invalidRequest = new LoginRequest("", "password123");

        mockMvc.perform(post(API_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("Should return 400 for login request with null password")
    void loginNullPasswordReturnsBadRequest() throws Exception {
        LoginRequest invalidRequest = new LoginRequest("testuser", null);

        mockMvc.perform(post(API_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("Should return 400 for invalid JSON in register request")
    void registerInvalidJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post(API_REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"username\": \"test\" }"))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Should return 400 for invalid JSON in login request")
    void loginInvalidJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post(API_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"username\": \"test\" }"))
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("Should return 415 for unsupported media type")
    void registerUnsupportedMediaTypeReturnsUnsupportedMediaType() throws Exception {
        mockMvc.perform(post(API_REGISTER)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("plain text"))
                .andExpect(status().isUnsupportedMediaType());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Should handle service exception during registration")
    void registerServiceThrowsExceptionReturnsAppropriateError() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException("Username already exists"));

        mockMvc.perform(post(API_REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegisterRequest)))
                .andExpect(status().isBadRequest()) // IllegalArgumentException → 400
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Username already exists"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Should handle service exception during login")
    void loginServiceThrowsExceptionReturnsAppropriateError() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("Invalid credentials"));

        mockMvc.perform(post(API_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isInternalServerError()) // RuntimeException → 500
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"));

        verify(authService).login(any(LoginRequest.class));
    }
}
