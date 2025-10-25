package com.notificationhub.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationhub.dto.response.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Maneja errores de autenticación (401 Unauthorized)
 */
@Component
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        log.error("Unauthorized error: {}", authException.getMessage());

        // Verificar si hay un error específico marcado por el filtro
        String authError = (String) request.getAttribute("auth.error");

        ErrorResponse errorResponse;

        if ("INVALID_TOKEN".equals(authError)) {
            errorResponse = ErrorResponse.builder()
                    .status(HttpServletResponse.SC_UNAUTHORIZED)
                    .error("Unauthorized")
                    .message("Invalid or expired JWT token")
                    .timestamp(LocalDateTime.now())
                    .path(request.getRequestURI())
                    .build();
        } else {
            errorResponse = ErrorResponse.builder()
                    .status(HttpServletResponse.SC_UNAUTHORIZED)
                    .error("Unauthorized")
                    .message("Authentication required")
                    .timestamp(LocalDateTime.now())
                    .path(request.getRequestURI())
                    .build();
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
