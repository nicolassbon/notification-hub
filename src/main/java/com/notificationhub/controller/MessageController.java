package com.notificationhub.controller;

import com.notificationhub.dto.request.MessageRequest;
import com.notificationhub.dto.response.MessageResponse;
import com.notificationhub.entity.Message;
import com.notificationhub.enums.DeliveryStatus;
import com.notificationhub.enums.PlatformType;
import com.notificationhub.mapper.MessageMapper;
import com.notificationhub.service.MessageService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/messages")
@Slf4j
public class MessageController {
    private final MessageService messageService;
    private final MessageMapper messageMapper;

    public MessageController(MessageService messageService, MessageMapper messageMapper) {
        this.messageService = messageService;
        this.messageMapper = messageMapper;
    }

    /**
     * Envía un mensaje a múltiples plataformas
     */
    @PostMapping("/send")
    public ResponseEntity<MessageResponse> sendMessage(@Valid @RequestBody MessageRequest request) {
        log.info("Received message send request for {} destinations", request.getDestinations().size());

        Message message = messageService.sendMessage(request);
        MessageResponse response = messageMapper.toResponse(message);

        log.info("Message sent successfully. ID: {}", message.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Obtiene todos los mensajes (Solo ADMIN)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MessageResponse>> getAllMessages() {
        log.info("Admin requesting all messages");

        List<Message> messages = messageService.getAllMessages();
        List<MessageResponse> responses = messageMapper.toResponseList(messages);

        log.info("Returning {} messages to admin", responses.size());
        return ResponseEntity.ok(responses);
    }

    /**
     * Obtiene los mensajes del usuario actual con filtros opcionales
     */
    @GetMapping("/my-messages")
    public ResponseEntity<List<MessageResponse>> getMyMessages(
            @RequestParam(required = false) DeliveryStatus status,
            @RequestParam(required = false) PlatformType platform,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        log.info("User requesting messages with filters - status: {}, platform: {}, from: {}, to: {}",
                status, platform, from, to);

        List<Message> messages = messageService.getUserMessagesWithFilters(status, platform, from, to);
        List<MessageResponse> responses = messageMapper.toResponseList(messages);

        log.info("Returning {} messages to user", responses.size());
        return ResponseEntity.ok(responses);
    }
}
