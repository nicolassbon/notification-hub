package com.notificationhub.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.notificationhub.security.handlers.JwtAccessDeniedHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAccessDeniedHandler Unit Tests")
public class JwtAccessDeniedHandlerTest {

    private JwtAccessDeniedHandler accessDeniedHandler;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        accessDeniedHandler = new JwtAccessDeniedHandler(objectMapper);
    }

    @Test
    @DisplayName("Should return 403 Forbidden with correct JSON structure")
    void handleReturnsForbiddenError() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(response.getWriter()).thenReturn(printWriter);

        AccessDeniedException exception = new AccessDeniedException("You shall not pass");

        accessDeniedHandler.handle(request, response, exception);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(response).setContentType("application/json");

        String responseContent = stringWriter.toString();

        assertTrue(responseContent.contains("\"status\":403"));
        assertTrue(responseContent.contains("\"error\":\"Forbidden\""));
        assertTrue(responseContent.contains("\"message\":\"Access denied: insufficient permissions\""));
    }
}
