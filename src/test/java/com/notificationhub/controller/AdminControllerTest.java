package com.notificationhub.controller;

import com.notificationhub.dto.response.MessageResponse;
import com.notificationhub.dto.response.MetricsResponse;
import com.notificationhub.entity.Message;
import com.notificationhub.entity.User;
import com.notificationhub.enums.Role;
import com.notificationhub.exception.handler.GlobalExceptionHandler;
import com.notificationhub.mapper.MessageMapper;
import com.notificationhub.service.MessageService;
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
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminController Unit Tests")
public class AdminControllerTest {

    private final String API_ADMIN_MESSAGES = "/api/admin/messages";
    private final String API_ADMIN_METRICS = "/api/admin/metrics";

    private MockMvc mockMvc;

    @Mock
    private MessageService messageService;

    @Mock
    private MessageMapper messageMapper;

    private Message testMessage;
    private MessageResponse testMessageResponse;
    private MetricsResponse testMetricsResponse;

    @BeforeEach
    void setUp() {
        AdminController adminController = new AdminController(messageService, messageMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(adminController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        User testUser = User.builder()
                .id(1L)
                .username("testuser")
                .role(Role.USER)
                .build();

        testMessage = Message.builder()
                .id(1L)
                .content("Test message content")
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .build();

        testMessageResponse = MessageResponse.builder()
                .id(1L)
                .content("Test message content")
                .username("testuser")
                .createdAt(LocalDateTime.now())
                .build();

        testMetricsResponse = MetricsResponse.builder()
                .username("testuser")
                .messagesSentToday(10)
                .remainingMessagesToday(90)
                .build();
    }

    // ==================== GET ALL MESSAGES TESTS ====================

    @Test
    @DisplayName("Should return all messages paginated with default params")
    void getAllMessagesDefaultParamsReturnsOk() throws Exception {
        Page<Message> messagePage = new PageImpl<>(List.of(testMessage), PageRequest.of(0, 20), 1);

        when(messageService.getAllMessages(any(Pageable.class))).thenReturn(messagePage);
        when(messageMapper.toResponse(any(Message.class))).thenReturn(testMessageResponse);

        mockMvc.perform(get(API_ADMIN_MESSAGES))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].username").value("testuser"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.size").value(20));

        verify(messageService).getAllMessages(any(Pageable.class));
        verify(messageMapper).toResponse(testMessage);
    }

    @Test
    @DisplayName("Should return all messages with custom pagination")
    void getAllMessagesCustomParamsReturnsOk() throws Exception {
        Page<Message> messagePage = new PageImpl<>(List.of(testMessage), PageRequest.of(1, 10), 11);

        when(messageService.getAllMessages(any(Pageable.class))).thenReturn(messagePage);
        when(messageMapper.toResponse(any(Message.class))).thenReturn(testMessageResponse);

        mockMvc.perform(get(API_ADMIN_MESSAGES)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(11));

        verify(messageService).getAllMessages(any(Pageable.class));
    }

    @Test
    @DisplayName("Should return empty page when no messages found")
    void getAllMessagesEmptyReturnsOk() throws Exception {
        Page<Message> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);

        when(messageService.getAllMessages(any(Pageable.class))).thenReturn(emptyPage);

        mockMvc.perform(get(API_ADMIN_MESSAGES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.empty").value(true));

        verify(messageService).getAllMessages(any(Pageable.class));
        verify(messageMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("Should handle service exception in getAllMessages")
    void getAllMessagesServiceErrorReturnsInternalServerError() throws Exception {
        when(messageService.getAllMessages(any(Pageable.class)))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get(API_ADMIN_MESSAGES))
                .andExpect(status().is5xxServerError());

        verify(messageService).getAllMessages(any(Pageable.class));
    }

    // ==================== GET METRICS TESTS ====================

    @Test
    @DisplayName("Should return metrics list successfully")
    void getMetricsReturnsOk() throws Exception {
        List<MetricsResponse> metricsList = Collections.singletonList(testMetricsResponse);
        when(messageService.getAllUserMetrics()).thenReturn(metricsList);

        mockMvc.perform(get(API_ADMIN_METRICS))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value("testuser"))
                .andExpect(jsonPath("$[0].messagesSentToday").value(10))
                .andExpect(jsonPath("$[0].remainingMessagesToday").value(90));

        verify(messageService).getAllUserMetrics();
    }

    @Test
    @DisplayName("Should return empty list when no metrics available")
    void getMetricsEmptyReturnsOk() throws Exception {
        when(messageService.getAllUserMetrics()).thenReturn(Collections.emptyList());

        mockMvc.perform(get(API_ADMIN_METRICS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(messageService).getAllUserMetrics();
    }

    @Test
    @DisplayName("Should handle service exception in getMetrics")
    void getMetricsServiceErrorReturnsInternalServerError() throws Exception {
        when(messageService.getAllUserMetrics())
                .thenThrow(new RuntimeException("Service failure"));

        mockMvc.perform(get(API_ADMIN_METRICS))
                .andExpect(status().is5xxServerError());

        verify(messageService).getAllUserMetrics();
    }
}
