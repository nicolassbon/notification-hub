package com.notificationhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationhub.dto.request.MessageRequest;
import com.notificationhub.dto.response.MessageResponse;
import com.notificationhub.dto.request.DestinationRequest;
import com.notificationhub.dto.response.MessageDeliveryResponse;
import com.notificationhub.entity.*;
import com.notificationhub.enums.DeliveryStatus;
import com.notificationhub.enums.PlatformType;
import com.notificationhub.enums.Role;
import com.notificationhub.exception.handler.GlobalExceptionHandler;
import com.notificationhub.service.MessageService;
import com.notificationhub.mapper.MessageMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageController Unit Tests")
class MessageControllerTest {

    private final String API_MESSAGES_GET = "/api/messages";
    private final String API_MESSAGES_SEND = "/api/messages/send";
    private MockMvc mockMvc;

    @Mock
    private MessageService messageService;

    @Mock
    private MessageMapper messageMapper;

    private ObjectMapper objectMapper;

    private MessageRequest validMessageRequest;
    private Message successMessage;
    private MessageResponse successResponse;

    @BeforeEach
    void setUp() {
        MessageController messageController = new MessageController(messageService, messageMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(messageController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        User testUser = User.builder()
                .id(1L)
                .username("testuser")
                .role(Role.USER)
                .build();

        validMessageRequest = new MessageRequest(
                "Test message content",
                Arrays.asList(
                        new DestinationRequest(PlatformType.DISCORD, null),
                        new DestinationRequest(PlatformType.TELEGRAM, "-6614987624")
                )
        );

        Map<String, Object> discordProviderResponse = new HashMap<>();
        discordProviderResponse.put("status", "success");
        discordProviderResponse.put("timestamp", LocalDateTime.now().toString());

        Map<String, Object> telegramProviderResponse = new HashMap<>();
        telegramProviderResponse.put("ok", true);
        telegramProviderResponse.put("message_id", 123);

        MessageDelivery discordDelivery = MessageDelivery.builder()
                .id(1L)
                .platformType(PlatformType.DISCORD)
                .destination(null)
                .status(DeliveryStatus.SUCCESS)
                .providerResponse(discordProviderResponse)
                .sentAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        MessageDelivery telegramDelivery = MessageDelivery.builder()
                .id(2L)
                .platformType(PlatformType.TELEGRAM)
                .destination("-6614987624")
                .status(DeliveryStatus.SUCCESS)
                .providerResponse(telegramProviderResponse)
                .sentAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        successMessage = Message.builder()
                .id(1L)
                .content("Test message content")
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .deliveries(Arrays.asList(discordDelivery, telegramDelivery))
                .build();

        MessageDeliveryResponse discordDeliveryResponse = MessageDeliveryResponse.builder()
                .id(1L)
                .platform(PlatformType.DISCORD)
                .destination(null)
                .status(DeliveryStatus.SUCCESS)
                .providerResponse(discordProviderResponse)
                .sentAt(LocalDateTime.now())
                .build();

        MessageDeliveryResponse telegramDeliveryResponse = MessageDeliveryResponse.builder()
                .id(2L)
                .platform(PlatformType.TELEGRAM)
                .destination("-6614987624")
                .status(DeliveryStatus.SUCCESS)
                .providerResponse(telegramProviderResponse)
                .sentAt(LocalDateTime.now())
                .build();

        successResponse = MessageResponse.builder()
                .id(1L)
                .content("Test message content")
                .username("testuser")
                .createdAt(LocalDateTime.now())
                .deliveries(Arrays.asList(discordDeliveryResponse, telegramDeliveryResponse))
                .build();
    }

    @Test
    @DisplayName("Should send message successfully and return 201 CREATED")
    void sendMessageValidRequestReturnsCreated() throws Exception {
        when(messageService.sendMessage(any(MessageRequest.class))).thenReturn(successMessage);
        when(messageMapper.toResponse(successMessage)).thenReturn(successResponse);

        mockMvc.perform(post(API_MESSAGES_SEND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validMessageRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.content").value("Test message content"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.deliveries").isArray())
                .andExpect(jsonPath("$.deliveries.length()").value(2))
                .andExpect(jsonPath("$.deliveries[0].platform").value("DISCORD"))
                .andExpect(jsonPath("$.deliveries[0].destination").isEmpty())
                .andExpect(jsonPath("$.deliveries[1].platform").value("TELEGRAM"))
                .andExpect(jsonPath("$.deliveries[1].destination").value("-6614987624"));

        verify(messageService).sendMessage(any(MessageRequest.class));
        verify(messageMapper).toResponse(successMessage);
    }

    @Test
    @DisplayName("Should include providerResponse in delivery response")
    void sendMessageIncludesProviderResponse() throws Exception {
        when(messageService.sendMessage(any(MessageRequest.class))).thenReturn(successMessage);
        when(messageMapper.toResponse(successMessage)).thenReturn(successResponse);

        mockMvc.perform(post(API_MESSAGES_SEND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validMessageRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deliveries[0].providerResponse").exists())
                .andExpect(jsonPath("$.deliveries[0].providerResponse.status").value("success"))
                .andExpect(jsonPath("$.deliveries[1].providerResponse").exists())
                .andExpect(jsonPath("$.deliveries[1].providerResponse.ok").value(true));

        verify(messageService).sendMessage(any(MessageRequest.class));
        verify(messageMapper).toResponse(successMessage);
    }

    @Test
    @DisplayName("Should return 400 for empty content")
    void sendMessageEmptyContentReturnsBadRequest() throws Exception {
        MessageRequest invalidRequest = new MessageRequest("", validMessageRequest.getDestinations());

        mockMvc.perform(post(API_MESSAGES_SEND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(messageService, never()).sendMessage(any());
        verify(messageMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("Should return 400 for null content")
    void sendMessageNullContentReturnsBadRequest() throws Exception {
        MessageRequest invalidRequest = new MessageRequest(null, validMessageRequest.getDestinations());

        mockMvc.perform(post(API_MESSAGES_SEND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(messageService, never()).sendMessage(any());
        verify(messageMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("Should return 400 for empty destinations list")
    void sendMessageEmptyDestinationsReturnsBadRequest() throws Exception {
        MessageRequest invalidRequest = new MessageRequest("Test message", List.of());

        mockMvc.perform(post(API_MESSAGES_SEND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(messageService, never()).sendMessage(any());
        verify(messageMapper, never()).toResponse(any());
    }


    @Test
    @DisplayName("Should return 400 for null destinations")
    void sendMessageNullDestinationsReturnsBadRequest() throws Exception {
        MessageRequest invalidRequest = new MessageRequest("Test message", null);

        mockMvc.perform(post(API_MESSAGES_SEND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(messageService, never()).sendMessage(any());
        verify(messageMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("Should return 400 for destination without platform")
    void sendMessageDestinationWithoutPlatformReturnsBadRequest() throws Exception {
        MessageRequest invalidRequest = new MessageRequest(
                "Test message",
                List.of(new DestinationRequest(null, "some-destination"))
        );

        mockMvc.perform(post(API_MESSAGES_SEND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(messageService, never()).sendMessage(any());
        verify(messageMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("Should return 400 for content exceeding 4000 characters")
    void sendMessageContentTooLongReturnsBadRequest() throws Exception {
        String longContent = "A".repeat(4001);
        MessageRequest invalidRequest = new MessageRequest(longContent, validMessageRequest.getDestinations());

        mockMvc.perform(post(API_MESSAGES_SEND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(messageService, never()).sendMessage(any());
        verify(messageMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("Should handle invalid JSON gracefully")
    void sendMessageInvalidJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post(API_MESSAGES_SEND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"invalid\": \"json\" }"))
                .andExpect(status().isBadRequest());

        verify(messageService, never()).sendMessage(any());
        verify(messageMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("Should handle service exception gracefully")
    void sendMessageServiceThrowsExceptionReturnsInternalServerError() throws Exception {
        when(messageService.sendMessage(any(MessageRequest.class)))
                .thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(post(API_MESSAGES_SEND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validMessageRequest)))
                .andExpect(status().is5xxServerError());

        verify(messageService).sendMessage(any(MessageRequest.class));
        verify(messageMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("Should return 415 for unsupported media type")
    void sendMessageUnsupportedMediaTypeReturnsUnsupportedMediaType() throws Exception {
        mockMvc.perform(post(API_MESSAGES_SEND)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("plain text"))
                .andExpect(status().isUnsupportedMediaType());

        verify(messageService, never()).sendMessage(any());
        verify(messageMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("Should return user messages without filters")
    void getMyMessagesWithoutFiltersReturnsOk() throws Exception {
        org.springframework.data.domain.Page<Message> messagePage = createMockPage(
                List.of(successMessage),
                0,
                20,
                1
        );

        when(messageService.getUserMessagesWithFilters(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any(org.springframework.data.domain.Pageable.class)
        )).thenReturn(messagePage);
        when(messageMapper.toResponse(any(Message.class))).thenReturn(successResponse);

        mockMvc.perform(get(API_MESSAGES_GET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].username").value("testuser"));

        verify(messageService).getUserMessagesWithFilters(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any(org.springframework.data.domain.Pageable.class)
        );
    }

    @Test
    @DisplayName("Should return user messages with status filter")
    void getMyMessagesWithStatusFilterReturnsOk() throws Exception {
        org.springframework.data.domain.Page<Message> messagePage = createMockPage(
                List.of(successMessage),
                0,
                20,
                1
        );

        when(messageService.getUserMessagesWithFilters(
                eq(DeliveryStatus.SUCCESS),
                isNull(),
                isNull(),
                isNull(),
                any(org.springframework.data.domain.Pageable.class)
        )).thenReturn(messagePage);
        when(messageMapper.toResponse(any(Message.class))).thenReturn(successResponse);

        mockMvc.perform(get(API_MESSAGES_GET)
                        .param("status", "SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        verify(messageService).getUserMessagesWithFilters(
                eq(DeliveryStatus.SUCCESS),
                isNull(),
                isNull(),
                isNull(),
                any(org.springframework.data.domain.Pageable.class)
        );
    }

    @Test
    @DisplayName("Should return user messages with platform filter")
    void getMyMessagesWithPlatformFilterReturnsOk() throws Exception {
        org.springframework.data.domain.Page<Message> messagePage = createMockPage(
                List.of(successMessage),
                0,
                20,
                1
        );

        when(messageService.getUserMessagesWithFilters(
                isNull(),
                eq(PlatformType.DISCORD),
                isNull(),
                isNull(),
                any(org.springframework.data.domain.Pageable.class)
        )).thenReturn(messagePage);
        when(messageMapper.toResponse(any(Message.class))).thenReturn(successResponse);

        mockMvc.perform(get(API_MESSAGES_GET)
                        .param("platform", "DISCORD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        verify(messageService).getUserMessagesWithFilters(
                isNull(),
                eq(PlatformType.DISCORD),
                isNull(),
                isNull(),
                any(org.springframework.data.domain.Pageable.class)
        );
    }

    @Test
    @DisplayName("Should return user messages with date range filter")
    void getMyMessagesWithDateRangeReturnsOk() throws Exception {
        org.springframework.data.domain.Page<Message> messagePage = createMockPage(
                List.of(successMessage),
                0,
                20,
                1
        );

        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now();

        when(messageService.getUserMessagesWithFilters(
                isNull(),
                isNull(),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                any(org.springframework.data.domain.Pageable.class)
        )).thenReturn(messagePage);
        when(messageMapper.toResponse(any(Message.class))).thenReturn(successResponse);

        mockMvc.perform(get(API_MESSAGES_GET)
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        verify(messageService).getUserMessagesWithFilters(
                isNull(),
                isNull(),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                any(org.springframework.data.domain.Pageable.class)
        );
    }

    @Test
    @DisplayName("Should return user messages with all filters")
    void getMyMessagesWithAllFiltersReturnsOk() throws Exception {
        org.springframework.data.domain.Page<Message> messagePage = createMockPage(
                List.of(successMessage),
                0,
                20,
                1
        );

        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now();

        when(messageService.getUserMessagesWithFilters(
                eq(DeliveryStatus.SUCCESS),
                eq(PlatformType.TELEGRAM),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                any(org.springframework.data.domain.Pageable.class)
        )).thenReturn(messagePage);
        when(messageMapper.toResponse(any(Message.class))).thenReturn(successResponse);

        mockMvc.perform(get(API_MESSAGES_GET)
                        .param("status", "SUCCESS")
                        .param("platform", "TELEGRAM")
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        verify(messageService).getUserMessagesWithFilters(
                eq(DeliveryStatus.SUCCESS),
                eq(PlatformType.TELEGRAM),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                any(org.springframework.data.domain.Pageable.class)
        );
    }

    @Test
    @DisplayName("Should handle invalid date format")
    void getMyMessagesInvalidDateFormatReturnsBadRequest() throws Exception {
        mockMvc.perform(get(API_MESSAGES_GET)
                        .param("from", "invalid-date")
                        .param("to", "invalid-date"))
                .andExpect(status().isBadRequest());

        verify(messageService, never()).getUserMessagesWithFilters(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should handle service exception in getMyMessages")
    void getMyMessagesServiceThrowsExceptionReturnsError() throws Exception {
        when(messageService.getUserMessagesWithFilters(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any(org.springframework.data.domain.Pageable.class)
        )).thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(get(API_MESSAGES_GET))
                .andExpect(status().is5xxServerError());

        verify(messageService).getUserMessagesWithFilters(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any(org.springframework.data.domain.Pageable.class)
        );
    }

    @Test
    @DisplayName("Should return empty list when no messages found")
    void getMyMessagesNoMessagesReturnsEmptyList() throws Exception {
        org.springframework.data.domain.Page<Message> emptyPage = createMockPage(
                List.of(),
                0,
                20,
                0
        );

        when(messageService.getUserMessagesWithFilters(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any(org.springframework.data.domain.Pageable.class)
        )).thenReturn(emptyPage);

        mockMvc.perform(get(API_MESSAGES_GET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.empty").value(true));

        verify(messageService).getUserMessagesWithFilters(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any(org.springframework.data.domain.Pageable.class)
        );
    }

    // ==================== PAGINATION TESTS ====================

    @Test
    @DisplayName("Should return paginated messages with default page and size")
    void getMyMessagesPaginatedDefaultParams() throws Exception {
        // Arrange
        org.springframework.data.domain.Page<Message> messagePage = createMockPage(
                List.of(successMessage),
                0,
                20,
                1
        );

        when(messageService.getUserMessagesWithFilters(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any(org.springframework.data.domain.Pageable.class)
        )).thenReturn(messagePage);

        when(messageMapper.toResponse(any(Message.class))).thenReturn(successResponse);

        // Act & Assert
        mockMvc.perform(get(API_MESSAGES_GET)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));

        verify(messageService).getUserMessagesWithFilters(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any(org.springframework.data.domain.Pageable.class)
        );
    }

    @Test
    @DisplayName("Should return second page with correct pagination metadata")
    void getMyMessagesPaginatedSecondPage() throws Exception {
        // Arrange - simular página 2 de 3
        org.springframework.data.domain.Page<Message> messagePage = createMockPage(
                List.of(successMessage),
                1, // página 1 (segunda página, 0-indexed)
                20,
                50 // 50 elementos totales = 3 páginas
        );

        when(messageService.getUserMessagesWithFilters(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any(org.springframework.data.domain.Pageable.class)
        )).thenReturn(messagePage);

        when(messageMapper.toResponse(any(Message.class))).thenReturn(successResponse);

        // Act & Assert
        mockMvc.perform(get(API_MESSAGES_GET)
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.totalElements").value(50))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    @DisplayName("Should return paginated messages with custom page size")
    void getMyMessagesPaginatedCustomSize() throws Exception {
        List<Message> messages = Arrays.asList(successMessage, successMessage, successMessage);

        Page<Message> messagePage = createMockPage(
                messages,
                0,
                10,
                30
        );

        when(messageService.getUserMessagesWithFilters(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(messagePage);

        when(messageMapper.toResponse(any(Message.class))).thenReturn(successResponse);

        mockMvc.perform(get(API_MESSAGES_GET)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.numberOfElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    @DisplayName("Should return paginated messages with filters applied")
    void getMyMessagesPaginatedWithFilters() throws Exception {
        Page<Message> messagePage = createMockPage(
                List.of(successMessage),
                0,
                20,
                1
        );

        when(messageService.getUserMessagesWithFilters(
                eq(DeliveryStatus.SUCCESS),
                eq(PlatformType.TELEGRAM),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(messagePage);

        when(messageMapper.toResponse(any(Message.class))).thenReturn(successResponse);

        mockMvc.perform(get(API_MESSAGES_GET)
                        .param("status", "SUCCESS")
                        .param("platform", "TELEGRAM")
                        .param("from", "2025-01-01T00:00:00")
                        .param("to", "2025-12-31T23:59:59")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(messageService).getUserMessagesWithFilters(
                eq(DeliveryStatus.SUCCESS),
                eq(PlatformType.TELEGRAM),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                any(org.springframework.data.domain.Pageable.class)
        );
    }

    @Test
    @DisplayName("Should return empty page when no messages match pagination criteria")
    void getMyMessagesPaginatedEmptyPage() throws Exception {
        Page<Message> emptyPage = createMockPage(
                List.of(),
                0,
                20,
                0
        );

        when(messageService.getUserMessagesWithFilters(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any(org.springframework.data.domain.Pageable.class)
        )).thenReturn(emptyPage);

        // Act & Assert
        mockMvc.perform(get(API_MESSAGES_GET)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.empty").value(true));
    }

    private Page<Message> createMockPage(
            List<Message> content,
            int pageNumber,
            int pageSize,
            long totalElements
    ) {
        return new PageImpl<>(
                content,
                PageRequest.of(pageNumber, pageSize),
                totalElements
        );
    }
}