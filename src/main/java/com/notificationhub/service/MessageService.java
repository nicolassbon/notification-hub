package com.notificationhub.service;

import com.notificationhub.dto.request.MessageRequest;
import com.notificationhub.dto.response.MetricsResponse;
import com.notificationhub.entity.Message;
import com.notificationhub.enums.DeliveryStatus;
import com.notificationhub.enums.PlatformType;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageService {
    Message sendMessage(MessageRequest request);

    List<Message> getAllMessages();

    List<Message> getUserMessagesWithFilters(
            DeliveryStatus status,
            PlatformType platform,
            LocalDateTime from,
            LocalDateTime to);

    List<MetricsResponse> getAllUserMetrics();
}
