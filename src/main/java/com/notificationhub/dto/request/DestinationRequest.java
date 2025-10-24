package com.notificationhub.dto.request;

import com.notificationhub.enums.PlatformType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DestinationRequest {

    @NotNull(message = "Platform is required")
    private PlatformType platform;

    // Opcional: dependiendo del tipo de plataforma
    private String destination;
}
