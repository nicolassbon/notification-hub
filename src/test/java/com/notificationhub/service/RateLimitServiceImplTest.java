package com.notificationhub.service;

import com.notificationhub.entity.DailyMessageCount;
import com.notificationhub.entity.User;
import com.notificationhub.enums.Role;
import com.notificationhub.exception.RateLimitExceededException;
import com.notificationhub.repository.DailyMessageCountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitServiceImpl Unit Tests")
public class RateLimitServiceImplTest {

    @Mock
    private DailyMessageCountRepository dailyMessageCountRepository;

    private RateLimitServiceImpl rateLimitService;
    private User testUser;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitServiceImpl(dailyMessageCountRepository);

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .role(Role.USER)
                .dailyMessageLimit(100)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should allow message when under daily limit")
    void checkRateLimit_UnderLimit_AllowsMessage() {
        LocalDate today = LocalDate.now();
        DailyMessageCount count = DailyMessageCount.builder()
                .user(testUser)
                .date(today)
                .count(50)
                .build();

        when(dailyMessageCountRepository.findByUserAndDate(testUser, today))
                .thenReturn(Optional.of(count));

        assertDoesNotThrow(() -> rateLimitService.checkRateLimit(testUser));

        verify(dailyMessageCountRepository).findByUserAndDate(testUser, today);
    }

    @Test
    @DisplayName("Should throw exception when daily limit exceeded")
    void checkRateLimit_LimitExceeded_ThrowsException() {
        LocalDate today = LocalDate.now();
        DailyMessageCount count = DailyMessageCount.builder()
                .user(testUser)
                .date(today)
                .count(100)
                .build();

        when(dailyMessageCountRepository.findByUserAndDate(testUser, today))
                .thenReturn(Optional.of(count));

        RateLimitExceededException exception = assertThrows(RateLimitExceededException.class, () -> rateLimitService.checkRateLimit(testUser));

        assertEquals("Daily message limit exceeded for user: testuser. Limit: 100", exception.getMessage());
        verify(dailyMessageCountRepository).findByUserAndDate(testUser, today);
    }

    @Test
    @DisplayName("Should create new counter when none exists")
    void checkRateLimitNoCounterCreatesNewCounter() {
        LocalDate today = LocalDate.now();
        DailyMessageCount newCount = DailyMessageCount.builder()
                .user(testUser)
                .date(today)
                .count(0)
                .build();

        when(dailyMessageCountRepository.findByUserAndDate(testUser, today))
                .thenReturn(Optional.empty());
        when(dailyMessageCountRepository.save(any(DailyMessageCount.class)))
                .thenReturn(newCount);

        assertDoesNotThrow(() -> rateLimitService.checkRateLimit(testUser));

        verify(dailyMessageCountRepository).save(any(DailyMessageCount.class));
    }

    @Test
    @DisplayName("Should increment existing counter")
    void incrementCounterExistingCounterIncrementsCount() {
        LocalDate today = LocalDate.now();
        DailyMessageCount count = DailyMessageCount.builder()
                .user(testUser)
                .date(today)
                .count(5)
                .build();

        when(dailyMessageCountRepository.findByUserAndDate(testUser, today))
                .thenReturn(Optional.of(count));
        when(dailyMessageCountRepository.save(count))
                .thenReturn(count);

        rateLimitService.incrementCounter(testUser);

        assertEquals(6, count.getCount());
        verify(dailyMessageCountRepository).save(count);
    }

    @Test
    @DisplayName("Should create new counter when incrementing without existing counter")
    void incrementCounterNoCounterCreatesAndIncrements() {
        LocalDate today = LocalDate.now();

        when(dailyMessageCountRepository.findByUserAndDate(testUser, today))
                .thenReturn(Optional.empty());

        DailyMessageCount savedCounter = DailyMessageCount.builder()
                .user(testUser)
                .date(today)
                .count(1)
                .build();

        when(dailyMessageCountRepository.save(any(DailyMessageCount.class)))
                .thenReturn(savedCounter);

        rateLimitService.incrementCounter(testUser);

        verify(dailyMessageCountRepository, times(2)).save(any(DailyMessageCount.class));
        verify(dailyMessageCountRepository).save(argThat(counter ->
                counter.getCount() == 0
        ));
    }

    @Test
    @DisplayName("Should calculate remaining messages correctly")
    void getRemainingMessagesWithExistingCounterReturnsCorrectRemaining() {
        LocalDate today = LocalDate.now();
        DailyMessageCount count = DailyMessageCount.builder()
                .user(testUser)
                .date(today)
                .count(75)
                .build();

        when(dailyMessageCountRepository.findByUserAndDate(testUser, today))
                .thenReturn(Optional.of(count));

        int remaining = rateLimitService.getRemainingMessages(testUser);

        assertEquals(25, remaining);
    }

    @Test
    @DisplayName("Should return full limit when no counter exists")
    void getRemainingMessagesNoCounterReturnsFullLimit() {
        LocalDate today = LocalDate.now();
        DailyMessageCount newCount = DailyMessageCount.builder()
                .user(testUser)
                .date(today)
                .count(0)
                .build();

        when(dailyMessageCountRepository.findByUserAndDate(testUser, today))
                .thenReturn(Optional.empty());
        when(dailyMessageCountRepository.save(any(DailyMessageCount.class)))
                .thenReturn(newCount);

        int remaining = rateLimitService.getRemainingMessages(testUser);

        assertEquals(100, remaining);
        verify(dailyMessageCountRepository).save(any(DailyMessageCount.class));
    }

    @Test
    @DisplayName("Should return zero when limit is exceeded")
    void getRemainingMessagesLimitExceededReturnsZero() {
        LocalDate today = LocalDate.now();
        DailyMessageCount count = DailyMessageCount.builder()
                .user(testUser)
                .date(today)
                .count(150)
                .build();

        when(dailyMessageCountRepository.findByUserAndDate(testUser, today))
                .thenReturn(Optional.of(count));

        int remaining = rateLimitService.getRemainingMessages(testUser);

        assertEquals(0, remaining);
    }
}
