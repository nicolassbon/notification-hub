package com.notificationhub.service;

import com.notificationhub.dto.request.MessageRequest;
import com.notificationhub.dto.request.DestinationRequest;
import com.notificationhub.dto.response.MetricsResponse;
import com.notificationhub.entity.*;
import com.notificationhub.enums.DeliveryStatus;
import com.notificationhub.enums.PlatformType;
import com.notificationhub.enums.Role;
import com.notificationhub.exception.RateLimitExceededException;
import com.notificationhub.repository.DailyMessageCountRepository;
import com.notificationhub.repository.MessageRepository;
import com.notificationhub.repository.UserRepository;
import com.notificationhub.service.platform.PlatformService;
import com.notificationhub.service.platform.PlatformServiceFactory;
import com.notificationhub.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageServiceImpl Unit Tests")
public class MessageServiceImplTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private PlatformServiceFactory platformServiceFactory;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DailyMessageCountRepository dailyMessageCountRepository;

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private PlatformService discordService;

    @Mock
    private PlatformService telegramService;

    private MessageServiceImpl messageService;
    private User testUser;
    private MessageRequest validMessageRequest;

    @BeforeEach
    void setUp() {
        messageService = new MessageServiceImpl(
                messageRepository,
                platformServiceFactory,
                userRepository,
                dailyMessageCountRepository,
                rateLimitService,
                securityUtils
        );

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .role(Role.USER)
                .dailyMessageLimit(100)
                .build();

        validMessageRequest = new MessageRequest(
                "Test message content",
                Arrays.asList(
                        new DestinationRequest(PlatformType.DISCORD, null),
                        new DestinationRequest(PlatformType.TELEGRAM, "-4614987626")
                )
        );
    }

    @Test
    @DisplayName("Should send message successfully to multiple platforms")
    void sendMessage_ValidRequest_Success() {
        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(platformServiceFactory.getService(PlatformType.DISCORD)).thenReturn(discordService);
        when(platformServiceFactory.getService(PlatformType.TELEGRAM)).thenReturn(telegramService);

        MessageDelivery discordDelivery = MessageDelivery.builder()
                .platformType(PlatformType.DISCORD)
                .status(DeliveryStatus.SUCCESS)
                .build();

        MessageDelivery telegramDelivery = MessageDelivery.builder()
                .platformType(PlatformType.TELEGRAM)
                .status(DeliveryStatus.SUCCESS)
                .build();

        when(discordService.send(any(), any(), any())).thenReturn(discordDelivery);
        when(telegramService.send(any(), any(), any())).thenReturn(telegramDelivery);

        Message savedMessage = Message.builder()
                .id(1L)
                .user(testUser)
                .content("Test message content")
                .deliveries(Arrays.asList(discordDelivery, telegramDelivery))
                .build();

        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);

        Message result = messageService.sendMessage(validMessageRequest);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(2, result.getDeliveries().size());

        verify(rateLimitService).checkRateLimit(testUser);
        verify(rateLimitService).incrementCounter(testUser);
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    @DisplayName("Should handle platform service failure gracefully")
    void sendMessage_PlatformFails_ContinuesWithOtherPlatforms() {
        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(platformServiceFactory.getService(PlatformType.DISCORD)).thenReturn(discordService);
        when(platformServiceFactory.getService(PlatformType.TELEGRAM)).thenReturn(telegramService);

        MessageDelivery discordDelivery = MessageDelivery.builder()
                .platformType(PlatformType.DISCORD)
                .status(DeliveryStatus.FAILED)
                .errorMessage("Exception: Service unavailable")
                .build();

        MessageDelivery telegramDelivery = MessageDelivery.builder()
                .platformType(PlatformType.TELEGRAM)
                .status(DeliveryStatus.SUCCESS)
                .build();

        when(discordService.send(any(), any(), any())).thenThrow(new RuntimeException("Service unavailable"));
        when(telegramService.send(any(), any(), any())).thenReturn(telegramDelivery);

        Message savedMessage = Message.builder()
                .id(1L)
                .user(testUser)
                .content("Test message content")
                .deliveries(Arrays.asList(discordDelivery, telegramDelivery))
                .build();

        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);

        Message result = messageService.sendMessage(validMessageRequest);

        assertNotNull(result);
        assertEquals(2, result.getDeliveries().size());

        verify(rateLimitService).incrementCounter(testUser);
    }

    @Test
    @DisplayName("Should not increment rate limit when all deliveries fail")
    void sendMessage_AllDeliveriesFail_NoRateLimitIncrement() {
        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(platformServiceFactory.getService(PlatformType.DISCORD)).thenReturn(discordService);
        when(platformServiceFactory.getService(PlatformType.TELEGRAM)).thenReturn(telegramService);

        when(discordService.send(any(), any(), any())).thenThrow(new RuntimeException("Discord failed"));
        when(telegramService.send(any(), any(), any())).thenThrow(new RuntimeException("Telegram failed"));

        Message savedMessage = Message.builder()
                .id(1L)
                .user(testUser)
                .content("Test message content")
                .deliveries(Arrays.asList(
                        MessageDelivery.builder().platformType(PlatformType.DISCORD).status(DeliveryStatus.FAILED).build(),
                        MessageDelivery.builder().platformType(PlatformType.TELEGRAM).status(DeliveryStatus.FAILED).build()
                ))
                .build();

        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);

        Message result = messageService.sendMessage(validMessageRequest);

        assertNotNull(result);

        verify(rateLimitService, never()).incrementCounter(testUser);
    }

    @Test
    @DisplayName("Should throw exception when rate limit exceeded")
    void sendMessage_RateLimitExceeded_ThrowsException() {
        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        doThrow(new RateLimitExceededException("Limit exceeded"))
                .when(rateLimitService).checkRateLimit(testUser);

        assertThrows(RateLimitExceededException.class, () -> messageService.sendMessage(validMessageRequest));

        verify(platformServiceFactory, never()).getService(any());
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when no authenticated user")
    void sendMessage_NoAuthenticatedUser_ThrowsException() {
        when(securityUtils.getCurrentUser()).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> messageService.sendMessage(validMessageRequest));

        verify(rateLimitService, never()).checkRateLimit(any());
    }

    @Test
    @DisplayName("Should return all messages for admin")
    void getAllMessages_AdminUser_ReturnsAllMessages() {
        when(securityUtils.isAdmin()).thenReturn(true);

        List<Message> expectedMessages = Arrays.asList(
                Message.builder().id(1L).content("Message 1").build(),
                Message.builder().id(2L).content("Message 2").build()
        );
        when(messageRepository.findAll()).thenReturn(expectedMessages);

        List<Message> result = messageService.getAllMessages();

        assertEquals(2, result.size());
        verify(messageRepository).findAll();
    }

    @Test
    @DisplayName("Should throw exception when non-admin tries to get all messages")
    void getAllMessages_NonAdminUser_ThrowsException() {
        when(securityUtils.isAdmin()).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> messageService.getAllMessages());

        verify(messageRepository, never()).findAll();
    }

    @Test
    @DisplayName("Should return user messages with filters")
    void getUserMessagesWithFilters_WithFilters_ReturnsFilteredMessages() {
        when(securityUtils.getCurrentUser()).thenReturn(testUser);

        List<Message> userMessages = Arrays.asList(
                Message.builder().id(1L).user(testUser).content("Message 1").build(),
                Message.builder().id(2L).user(testUser).content("Message 2").build()
        );

        when(messageRepository.findByUserOrderByCreatedAtDesc(testUser)).thenReturn(userMessages);

        List<Message> result = messageService.getUserMessagesWithFilters(
                DeliveryStatus.SUCCESS, PlatformType.DISCORD, null, null
        );

        assertNotNull(result);
        verify(messageRepository).findByUserOrderByCreatedAtDesc(testUser);
    }

    @Test
    @DisplayName("Should return user messages with date range")
    void getUserMessagesWithFilters_WithDateRange_UsesDateRangeQuery() {
        when(securityUtils.getCurrentUser()).thenReturn(testUser);

        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now();
        List<Message> userMessages = List.of(
                Message.builder().id(1L).user(testUser).content("Message 1").build()
        );

        when(messageRepository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(testUser, from, to))
                .thenReturn(userMessages);

        List<Message> result = messageService.getUserMessagesWithFilters(null, null, from, to);

        assertNotNull(result);
        verify(messageRepository).findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(testUser, from, to);
    }

    @Test
    @DisplayName("Should return metrics for all users when admin")
    void getAllUserMetrics_AdminUser_ReturnsMetrics() {
        when(securityUtils.isAdmin()).thenReturn(true);

        User user1 = User.builder()
                .id(1L)
                .username("user1")
                .dailyMessageLimit(100)
                .build();

        User user2 = User.builder()
                .id(2L)
                .username("user2")
                .dailyMessageLimit(50)
                .build();

        List<User> users = Arrays.asList(user1, user2);
        when(userRepository.findAll()).thenReturn(users);

        // User1: 10 mensajes totales, 3 hoy
        when(messageRepository.countByUser(user1)).thenReturn(10L);
        DailyMessageCount count1 = DailyMessageCount.builder()
                .user(user1)
                .date(LocalDate.now())
                .count(3)
                .build();
        when(dailyMessageCountRepository.findByUserAndDate(user1, LocalDate.now()))
                .thenReturn(Optional.of(count1));

        // User2: 5 mensajes totales, ninguno hoy
        when(messageRepository.countByUser(user2)).thenReturn(5L);
        when(dailyMessageCountRepository.findByUserAndDate(user2, LocalDate.now()))
                .thenReturn(Optional.empty());

        List<MetricsResponse> result = messageService.getAllUserMetrics();

        assertNotNull(result);
        assertEquals(2, result.size());

        MetricsResponse metrics1 = result.stream()
                .filter(m -> m.getUsername().equals("user1"))
                .findFirst()
                .orElseThrow();
        assertEquals(10L, metrics1.getTotalMessagesSent());
        assertEquals(3, metrics1.getMessagesSentToday());
        assertEquals(97, metrics1.getRemainingMessagesToday());
        assertEquals(100, metrics1.getDailyLimit());

        MetricsResponse metrics2 = result.stream()
                .filter(m -> m.getUsername().equals("user2"))
                .findFirst()
                .orElseThrow();
        assertEquals(5L, metrics2.getTotalMessagesSent());
        assertEquals(0, metrics2.getMessagesSentToday());
        assertEquals(50, metrics2.getRemainingMessagesToday());
        assertEquals(50, metrics2.getDailyLimit());

        verify(userRepository).findAll();
        verify(messageRepository, times(2)).countByUser(any());
        verify(dailyMessageCountRepository, times(2)).findByUserAndDate(any(), any());
    }

    @Test
    @DisplayName("Should throw exception when non-admin tries to get metrics")
    void getAllUserMetrics_NonAdminUser_ThrowsException() {
        when(securityUtils.isAdmin()).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> messageService.getAllUserMetrics());

        verify(userRepository, never()).findAll();
        verify(messageRepository, never()).countByUser(any());
    }

    @Test
    @DisplayName("Should handle user with no messages in metrics")
    void getAllUserMetrics_UserWithNoMessages_ReturnsZeroMetrics() {
        when(securityUtils.isAdmin()).thenReturn(true);

        User newUser = User.builder()
                .id(1L)
                .username("newuser")
                .dailyMessageLimit(100)
                .build();

        when(userRepository.findAll()).thenReturn(List.of(newUser));
        when(messageRepository.countByUser(newUser)).thenReturn(0L);
        when(dailyMessageCountRepository.findByUserAndDate(newUser, LocalDate.now()))
                .thenReturn(Optional.empty());

        List<MetricsResponse> result = messageService.getAllUserMetrics();

        assertEquals(1, result.size());
        MetricsResponse metrics = result.get(0);
        assertEquals("newuser", metrics.getUsername());
        assertEquals(0L, metrics.getTotalMessagesSent());
        assertEquals(0, metrics.getMessagesSentToday());
        assertEquals(100, metrics.getRemainingMessagesToday());
    }

    @Test
    @DisplayName("Should handle user who reached daily limit")
    void getAllUserMetrics_UserReachedLimit_ShowsZeroRemaining() {
        when(securityUtils.isAdmin()).thenReturn(true);

        User user = User.builder()
                .id(1L)
                .username("poweruser")
                .dailyMessageLimit(10)
                .build();

        when(userRepository.findAll()).thenReturn(List.of(user));
        when(messageRepository.countByUser(user)).thenReturn(50L);

        DailyMessageCount count = DailyMessageCount.builder()
                .user(user)
                .date(LocalDate.now())
                .count(10)
                .build();
        when(dailyMessageCountRepository.findByUserAndDate(user, LocalDate.now()))
                .thenReturn(Optional.of(count));

        List<MetricsResponse> result = messageService.getAllUserMetrics();

        assertEquals(1, result.size());
        MetricsResponse metrics = result.get(0);
        assertEquals(10, metrics.getMessagesSentToday());
        assertEquals(0, metrics.getRemainingMessagesToday());
    }

    @Test
    @DisplayName("Should handle user who exceeded daily limit")
    void getAllUserMetrics_UserExceededLimit_ShowsZeroRemaining() {
        when(securityUtils.isAdmin()).thenReturn(true);

        User user = User.builder()
                .id(1L)
                .username("overuser")
                .dailyMessageLimit(5)
                .build();

        when(userRepository.findAll()).thenReturn(List.of(user));
        when(messageRepository.countByUser(user)).thenReturn(20L);

        DailyMessageCount count = DailyMessageCount.builder()
                .user(user)
                .date(LocalDate.now())
                .count(8)
                .build();
        when(dailyMessageCountRepository.findByUserAndDate(user, LocalDate.now()))
                .thenReturn(Optional.of(count));

        List<MetricsResponse> result = messageService.getAllUserMetrics();

        assertEquals(1, result.size());
        MetricsResponse metrics = result.get(0);
        assertEquals(8, metrics.getMessagesSentToday());
        assertEquals(0, metrics.getRemainingMessagesToday());
    }

    @Test
    @DisplayName("Should return empty list when no active users")
    void getAllUserMetrics_NoActiveUsers_ReturnsEmptyList() {
        when(securityUtils.isAdmin()).thenReturn(true);
        when(userRepository.findAll()).thenReturn(List.of());

        List<MetricsResponse> result = messageService.getAllUserMetrics();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepository).findAll();
        verify(messageRepository, never()).countByUser(any());
    }
}
