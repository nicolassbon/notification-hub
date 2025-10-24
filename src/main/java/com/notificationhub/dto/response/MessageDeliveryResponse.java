package com.notificationhub.dto.response;

import com.notificationhub.enums.DeliveryStatus;
import com.notificationhub.enums.PlatformType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDeliveryResponse {
    private Long id;
    private PlatformType platform;
    private String destination;
    private DeliveryStatus status;
    private Map<String, Object> providerResponse;
    private String errorMessage;
    private LocalDateTime sentAt;
}
