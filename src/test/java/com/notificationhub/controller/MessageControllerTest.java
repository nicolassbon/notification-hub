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
import com.notificationhub.exception.GlobalExceptionHandler;
import com.notificationhub.service.MessageService;
import com.notificationhub.mapper.MessageMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageController Unit Tests - Send Endpoint")
class MessageControllerTest {

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

        mockMvc.perform(post("/api/messages/send")
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

        mockMvc.perform(post("/api/messages/send")
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

        mockMvc.perform(post("/api/messages/send")
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

        mockMvc.perform(post("/api/messages/send")
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

        mockMvc.perform(post("/api/messages/send")
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

        mockMvc.perform(post("/api/messages/send")
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

        mockMvc.perform(post("/api/messages/send")
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

        mockMvc.perform(post("/api/messages/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(messageService, never()).sendMessage(any());
        verify(messageMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("Should handle invalid JSON gracefully")
    void sendMessageInvalidJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/messages/send")
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

        mockMvc.perform(post("/api/messages/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validMessageRequest)))
                .andExpect(status().is5xxServerError());

        verify(messageService).sendMessage(any(MessageRequest.class));
        verify(messageMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("Should return 415 for unsupported media type")
    void sendMessageUnsupportedMediaTypeReturnsUnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/api/messages/send")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("plain text"))
                .andExpect(status().isUnsupportedMediaType());

        verify(messageService, never()).sendMessage(any());
        verify(messageMapper, never()).toResponse(any());
    }
}