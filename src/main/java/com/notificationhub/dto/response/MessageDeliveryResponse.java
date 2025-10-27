package com.notificationhub.dto.response;

import com.notificationhub.enums.DeliveryStatus;
import com.notificationhub.enums.PlatformType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Message delivery status for a specific platform")
public class MessageDeliveryResponse {
    @Schema(description = "Delivery ID", example = "1")
    private Long id;

    @Schema(description = "Target platform", example = "TELEGRAM")
    private PlatformType platform;

    @Schema(description = "Destination identifier", example = "123456789")
    private String destination;

    @Schema(description = "Delivery status", example = "SUCCESS", allowableValues = {"PENDING", "SUCCESS", "FAILED"})
    private DeliveryStatus status;

    @Schema(description = "Provider response data")
    private Map<String, Object> providerResponse;

    @Schema(description = "Error message if delivery failed", example = "Invalid chat ID")
    private String errorMessage;

    @Schema(description = "When the message was actually sent")
    private LocalDateTime sentAt;
}
