package com.notificationhub.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Authentication response with JWT token")
public class AuthResponse {
    @Schema(description = "JWT token for authenticated requests", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    @Schema(description = "Authenticated username", example = "nico")
    private String username;

    @Schema(description = "User role", example = "USER", allowableValues = {"USER", "ADMIN"})
    private String role;

    @Schema(description = "Token expiration time in milliseconds", example = "86400000")
    private Long expiresIn;
}
