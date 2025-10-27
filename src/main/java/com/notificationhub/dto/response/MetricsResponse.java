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
@Schema(description = "User metrics for admin dashboard")
public class MetricsResponse {
    @Schema(description = "Username", example = "admin")
    private String username;

    @Schema(description = "Total messages sent by user", example = "150")
    private Long totalMessagesSent;

    @Schema(description = "Messages sent today", example = "5")
    private Integer messagesSentToday;

    @Schema(description = "Remaining messages for today", example = "95")
    private Integer remainingMessagesToday;

    @Schema(description = "Daily message limit", example = "100")
    private Integer dailyLimit;
}
