package com.notificationhub.service.impl;

import com.notificationhub.entity.DailyMessageCount;
import com.notificationhub.entity.User;
import com.notificationhub.exception.custom.RateLimitExceededException;
import com.notificationhub.repository.DailyMessageCountRepository;
import com.notificationhub.service.RateLimitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Slf4j
@Transactional
public class RateLimitServiceImpl implements RateLimitService {
    private final DailyMessageCountRepository dailyMessageCountRepository;

    public RateLimitServiceImpl(DailyMessageCountRepository dailyMessageCountRepository) {
        this.dailyMessageCountRepository = dailyMessageCountRepository;
    }

    public void checkRateLimit(User user) {
        LocalDate today = LocalDate.now();

        DailyMessageCount count = dailyMessageCountRepository
                .findByUserAndDate(user, today)
                .orElseGet(() -> createNewCounter(user, today));

        if (count.hasReachedLimit(user.getDailyMessageLimit())) {
            log.warn("User {} has reached daily limit. Count: {}, Limit: {}",
                    user.getUsername(), count.getCount(), user.getDailyMessageLimit());

            throw new RateLimitExceededException(
                    "Daily message limit exceeded for user: " + user.getUsername() + ". Limit: " + user.getDailyMessageLimit()
            );
        }
    }

    @CacheEvict(value = "rateLimits", key = "#user.id + '_' + T(java.time.LocalDate).now()")
    public void incrementCounter(User user) {
        LocalDate today = LocalDate.now();

        int rowsUpdated = dailyMessageCountRepository.incrementCountAtomic(user, today);

        if (rowsUpdated == 0) {
            createNewCounter(user, today);
            dailyMessageCountRepository.incrementCountAtomic(user, today);
        }

        log.info("Incremented message counter for user {} atomically", user.getUsername());
    }

    @Cacheable(value = "rateLimits", key = "#user.id + '_' + T(java.time.LocalDate).now()")
    public int getRemainingMessages(User user) {
        LocalDate today = LocalDate.now();

        DailyMessageCount count = dailyMessageCountRepository
                .findByUserAndDate(user, today)
                .orElseGet(() -> createNewCounter(user, today));

        return count.getRemainingMessages(user.getDailyMessageLimit());
    }

    private DailyMessageCount createNewCounter(User user, LocalDate date) {
        log.info("Creating new daily message counter for user {} on {}", user.getUsername(), date);

        DailyMessageCount newCount = DailyMessageCount.builder()
                .user(user)
                .date(date)
                .count(0)
                .build();

        return dailyMessageCountRepository.save(newCount);
    }
}
