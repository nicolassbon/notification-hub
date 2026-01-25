package com.notificationhub.exception.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationhub.dto.request.LoginRequest;
import com.notificationhub.exception.custom.InvalidCredentialsException;
import com.notificationhub.exception.custom.MessageDeliveryException;
import com.notificationhub.exception.custom.RateLimitExceededException;
import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GlobalExceptionHandler Unit Tests")
public class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @RestController
    static class TestController {

        @GetMapping("/test/rate-limit")
        public void throwRateLimit() {
            throw new RateLimitExceededException("You have exceeded the request limit.");
        }

        @GetMapping("/test/illegal-argument")
        public void throwIllegalArgument() {
            throw new IllegalArgumentException("Invalid argument provided.");
        }

        @GetMapping("/test/message-delivery")
        public void throwMessageDelivery() {
            throw new MessageDeliveryException("Failed to deliver message.");
        }

        @GetMapping("/test/invalid-credentials")
        public void throwInvalidCredentials() {
            throw new InvalidCredentialsException("Invalid username or password.");
        }

        @GetMapping("/test/type-mismatch/{id}")
        public void handleTypeMismatch(@PathVariable Long id) {
            // Empty method - used only to test MethodArgumentTypeMismatchException
        }

        @GetMapping("/test/datetime-parse")
        public void throwDateTimeParse(@RequestParam String date) {
            LocalDateTime.parse(date);
        }

        @PostMapping("/test/validation")
        public void handleValidation(@RequestBody @Valid LoginRequest request) {
            // Empty method - used only to test MethodArgumentNotValidException
        }

        @PostMapping(value = "/test/media-type", consumes = "application/xml")
        public void handleMediaType() {
            // Empty method - used only to test HttpMediaTypeNotSupportedException
        }

        @GetMapping("/test/method-not-allowed")
        public void onlyGet() {
            // Empty method - used only to test HttpRequestMethodNotSupportedException
        }

        @GetMapping("/test/generic-exception")
        public void throwGenericException() {
            throw new RuntimeException("Unexpected error");
        }
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ========== 4xx CLIENT ERRORS ==========

    @Test
    @DisplayName("Should handle RateLimitExceededException and return 429 TOO_MANY_REQUESTS")
    void shouldHandleRateLimitExceededException() throws Exception {
        mockMvc.perform(get("/test/rate-limit")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Rate Limit Exceeded"))
                .andExpect(jsonPath("$.message").value("You have exceeded the request limit."))
                .andExpect(jsonPath("$.timestamp").value(notNullValue()));
    }

    @Test
    @DisplayName("Should handle IllegalArgumentException and return 400 BAD_REQUEST")
    void shouldHandleIllegalArgumentException() throws Exception {
        mockMvc.perform(get("/test/illegal-argument")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid argument provided."))
                .andExpect(jsonPath("$.timestamp").value(notNullValue()));
    }

    @Test
    @DisplayName("Should handle MessageDeliveryException and return 400 BAD_REQUEST")
    void shouldHandleMessageDeliveryException() throws Exception {
        mockMvc.perform(get("/test/message-delivery")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Failed to deliver message."))
                .andExpect(jsonPath("$.timestamp").value(notNullValue()));
    }

    @Test
    @DisplayName("Should handle InvalidCredentialsException and return 401 UNAUTHORIZED")
    void shouldHandleInvalidCredentialsException() throws Exception {
        mockMvc.perform(get("/test/invalid-credentials")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid username or password."))
                .andExpect(jsonPath("$.timestamp").value(notNullValue()));
    }

    @Test
    @DisplayName("Should handle MethodArgumentTypeMismatchException and return 400 BAD_REQUEST")
    void shouldHandleMethodArgumentTypeMismatchException() throws Exception {
        mockMvc.perform(get("/test/type-mismatch/invalid-id")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid parameter format"))
                .andExpect(jsonPath("$.timestamp").value(notNullValue()));
    }

    @Test
    @DisplayName("Should handle DateTimeParseException and return 400 BAD_REQUEST")
    void shouldHandleDateTimeParseException() throws Exception {
        mockMvc.perform(get("/test/datetime-parse")
                        .param("date", "invalid-date")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid date/time format"))
                .andExpect(jsonPath("$.timestamp").value(notNullValue()));
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException and return 400 BAD_REQUEST with validation details")
    void shouldHandleMethodArgumentNotValidException() throws Exception {
        LoginRequest invalidRequest = new LoginRequest("", "");

        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.message").value("Invalid request data"))
                .andExpect(jsonPath("$.timestamp").value(notNullValue()))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    @DisplayName("Should handle HttpMediaTypeNotSupportedException and return 415 UNSUPPORTED_MEDIA_TYPE")
    void shouldHandleHttpMediaTypeNotSupportedException() throws Exception {
        mockMvc.perform(post("/test/media-type")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.error").value("Unsupported Media Type"))
                .andExpect(jsonPath("$.message").value(notNullValue()))
                .andExpect(jsonPath("$.timestamp").value(notNullValue()));
    }

    @Test
    @DisplayName("Should handle HttpRequestMethodNotSupportedException and return 405 METHOD_NOT_ALLOWED")
    void shouldHandleHttpRequestMethodNotSupportedException() throws Exception {
        mockMvc.perform(post("/test/method-not-allowed")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").value("Method Not Allowed"))
                .andExpect(jsonPath("$.message").value("Method 'POST' not supported for this endpoint"))
                .andExpect(jsonPath("$.timestamp").value(notNullValue()));
    }

    @Test
    @DisplayName("Should handle HttpMessageNotReadableException and return 400 BAD_REQUEST")
    void shouldHandleHttpMessageNotReadableException() throws Exception {
        String malformedJson = "{ malformed json without quotes }";

        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Malformed JSON request"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    // ========== 5xx SERVER ERRORS ==========

    @Test
    @DisplayName("Should handle generic Exception and return 500 INTERNAL_SERVER_ERROR")
    void shouldHandleGenericException() throws Exception {
        mockMvc.perform(get("/test/generic-exception")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.timestamp").value(notNullValue()));
    }
}
