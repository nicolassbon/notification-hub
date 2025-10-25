package com.notificationhub.service;

import com.notificationhub.entity.User;

public interface RateLimitService {
    void checkRateLimit(User user);

    void incrementCounter(User user);

    int getRemainingMessages(User user);
}
