package com.notificationhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private String type = "Bearer";
    private String username;
    private String role;
    private Long expiresIn;

    public AuthResponse(String token, String username, String role, Long expiresIn) {
        this.token = token;
        this.username = username;
        this.role = role;
        this.expiresIn = expiresIn;
    }
}
