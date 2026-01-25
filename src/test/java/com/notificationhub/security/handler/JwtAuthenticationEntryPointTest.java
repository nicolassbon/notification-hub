package com.notificationhub.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.notificationhub.security.handlers.JwtAuthenticationEntryPoint;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.AuthenticationException;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationEntryPoint Unit Tests")
public class JwtAuthenticationEntryPointTest {

    private JwtAuthenticationEntryPoint authEntryPoint;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private AuthenticationException authException;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        authEntryPoint = new JwtAuthenticationEntryPoint(objectMapper);
    }

    @Test
    @DisplayName("Should return 'Invalid Token' message when auth.error is INVALID_TOKEN")
    void commenceWithInvalidTokenReturnsSpecificMessage() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        when(request.getAttribute("auth.error")).thenReturn("INVALID_TOKEN");
        when(authException.getMessage()).thenReturn("Token signature invalid");

        authEntryPoint.commence(request, response, authException);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");

        String responseContent = stringWriter.toString();
        assertTrue(responseContent.contains("\"message\":\"Invalid or expired JWT token\""));
        assertTrue(responseContent.contains("\"status\":401"));
    }

    @Test
    @DisplayName("Should return generic 'Authentication required' message when no specific error attribute")
    void commenceWithoutSpecificErrorReturnsGenericMessage() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        when(request.getAttribute("auth.error")).thenReturn(null);
        when(authException.getMessage()).thenReturn("Full authentication is required");

        authEntryPoint.commence(request, response, authException);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        String responseContent = stringWriter.toString();
        assertTrue(responseContent.contains("\"message\":\"Authentication required\""));
        assertTrue(responseContent.contains("\"status\":401"));
    }
}
