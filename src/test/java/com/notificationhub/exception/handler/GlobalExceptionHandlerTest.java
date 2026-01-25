package com.notificationhub.exception.handler;

import com.notificationhub.exception.custom.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GlobalExceptionHandler Unit Tests")
public class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @RestController
    static class TestController {
        @GetMapping("/test/rate-limit")
        public void throwRateLimit() {
            throw new RateLimitExceededException("You have exceeded the request limit.");
        }

        @GetMapping("/test/bad-request")
        public void throwIllegalArgument() {
            throw new IllegalArgumentException("Invalid argument provided.");
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Should handle RateLimitExceededException and return 429 TOO_MANY_REQUESTS")
    void shouldHandleRateLimitExceededException() throws Exception {
        mockMvc.perform(get("/test/rate-limit")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Rate Limit Exceeded"))
                .andExpect(jsonPath("$.message").value("You have exceeded the request limit."));
    }

    @Test
    @DisplayName("Should handle IllegalArgumentException and return 400 BAD_REQUEST")
    void shouldHandleIllegalArgumentException() throws Exception {
        mockMvc.perform(get("/test/bad-request")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid argument provided."));
    }
}
