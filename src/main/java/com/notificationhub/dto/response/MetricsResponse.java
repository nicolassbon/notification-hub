package com.notificationhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetricsResponse {
    private String username;
    private Long totalMessagesSent;
    private Integer messagesSentToday;
    private Integer remainingMessagesToday;
    private Integer dailyLimit;
}
