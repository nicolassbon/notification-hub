package com.notificationhub.dto.criteria;

import com.notificationhub.entity.User;
import com.notificationhub.enums.DeliveryStatus;
import com.notificationhub.enums.PlatformType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record MessageFilterCriteria(
        User user,
        DeliveryStatus status,
        PlatformType platform,
        LocalDateTime from,
        LocalDateTime to
) {

    public boolean hasDateFilters() {
        return from != null && to != null;
    }

    public static MessageFilterCriteria empty() {
        return MessageFilterCriteria.builder().build();
    }
}
