package com.notificationhub.dto.request;

import com.notificationhub.enums.PlatformType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Message destination configuration")
public class DestinationRequest {

    @NotNull(message = "Platform is required")
    @Schema(description = "Target platform", example = "TELEGRAM", allowableValues = {"TELEGRAM", "DISCORD"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private PlatformType platform;

    @Schema(description = "Platform-specific destination (channel ID, chat ID, etc.). If empty, uses default configuration.")
    private String destination;
}
