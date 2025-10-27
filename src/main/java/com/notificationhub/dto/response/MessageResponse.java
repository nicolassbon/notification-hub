package com.notificationhub.dto.response;

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
@Schema(description = "Message response with delivery status")
public class MessageResponse {
    @Schema(description = "Message ID", example = "1")
    private Long id;

    @Schema(description = "Message content", example = "Hello from Notification Hub!")
    private String content;

    @Schema(description = "Sender username", example = "nico")
    private String username;

    @Schema(description = "Creation timestamp", example = "2025-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Delivery attempts to different platforms")
    private List<MessageDeliveryResponse> deliveries;
}
