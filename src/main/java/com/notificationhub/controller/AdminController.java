package com.notificationhub.controller;

import com.notificationhub.dto.response.MessageResponse;
import com.notificationhub.dto.response.MetricsResponse;
import com.notificationhub.entity.Message;
import com.notificationhub.mapper.MessageMapper;
import com.notificationhub.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminController {
    private final MessageService messageService;
    private final MessageMapper messageMapper;

    public AdminController(MessageService messageService, MessageMapper messageMapper) {
        this.messageService = messageService;
        this.messageMapper = messageMapper;
    }

    /**
     * Obtiene todos los mensajes del sistema (solo ADMIN)
     */
    @GetMapping("/messages")
    public ResponseEntity<List<MessageResponse>> getAllMessages() {
        log.info("Admin requesting all messages");

        List<Message> messages = messageService.getAllMessages();
        List<MessageResponse> responses = messageMapper.toResponseList(messages);

        log.info("Returning {} messages to admin", responses.size());
        return ResponseEntity.ok(responses);
    }

    /**
     * Obtiene métricas de todos los usuarios (solo ADMIN)
     * Requirement: "access a special metrics endpoint"
     */
    @GetMapping("/metrics")
    public ResponseEntity<List<MetricsResponse>> getMetrics() {
        log.info("Admin requesting user metrics");

        // TODO: Implementar después
        // List<MetricsResponse> metrics = metricsService.getAllUserMetrics();

        return ResponseEntity.ok(List.of());
    }
}
