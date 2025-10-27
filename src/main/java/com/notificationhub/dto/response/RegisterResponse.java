package com.notificationhub.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User registration response")
public class RegisterResponse {
    @Schema(description = "Success message", example = "User registered successfully")
    private String message;

    @Schema(description = "Registration timestamp", example = "2025-01-15T10:30:00")
    private LocalDateTime timestamp;

    @Schema(description = "Registered username", example = "nico")
    private String username;
}
