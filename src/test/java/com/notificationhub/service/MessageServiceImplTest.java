package com.notificationhub.service;

import com.notificationhub.dto.request.MessageRequest;
import com.notificationhub.dto.request.DestinationRequest;
import com.notificationhub.entity.*;
import com.notificationhub.enums.DeliveryStatus;
import com.notificationhub.enums.PlatformType;
import com.notificationhub.enums.Role;
import com.notificationhub.exception.RateLimitExceededException;
import com.notificationhub.repository.MessageRepository;
import com.notificationhub.service.platform.PlatformService;
import com.notificationhub.service.platform.PlatformServiceFactory;
import com.notificationhub.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

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
                messageRepository, platformServiceFactory, rateLimitService, securityUtils
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
}
