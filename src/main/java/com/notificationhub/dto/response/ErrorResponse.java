package com.notificationhub.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Standard error response")
public class ErrorResponse {
    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "Error type", example = "Validation Failed")
    private String error;

    @Schema(description = "Error message", example = "Invalid request data")
    private String message;

    @Schema(description = "Error timestamp", example = "2025-01-15T10:30:00")
    private LocalDateTime timestamp;

    // Solo incluir detalles si no es nulo
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Additional error details", example = "[\"Username must be at least 3 characters\"]")
    private List<String> details;
}
