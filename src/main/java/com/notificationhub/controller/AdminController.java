package com.notificationhub.controller;

import com.notificationhub.dto.response.MessageResponse;
import com.notificationhub.dto.response.MetricsResponse;
import com.notificationhub.entity.Message;
import com.notificationhub.mapper.MessageMapper;
import com.notificationhub.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Admin", description = "Administrative endpoints - ADMIN role required")
@Slf4j
public class AdminController {
    private final MessageService messageService;
    private final MessageMapper messageMapper;

    public AdminController(MessageService messageService, MessageMapper messageMapper) {
        this.messageService = messageService;
        this.messageMapper = messageMapper;
    }

    @GetMapping("/messages")
    @Operation(
            summary = "Get all messages (Admin only)",
            description = "Retrieve all messages from all users in the system. Only accessible by users with ADMIN role.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Messages retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated - JWT token required"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - ADMIN role required"
            )
    })
    public ResponseEntity<List<MessageResponse>> getAllMessages() {
        log.info("Admin requesting all messages");

        List<Message> messages = messageService.getAllMessages();
        List<MessageResponse> responses = messageMapper.toResponseList(messages);

        log.info("Returning {} messages to admin", responses.size());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/metrics")
    @Operation(
            summary = "Get system metrics (Admin only)",
            description = "Get metrics for all users including total messages sent and remaining daily quota. Only accessible by ADMIN.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Metrics retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - ADMIN role required"
            )
    })
    public ResponseEntity<List<MetricsResponse>> getMetrics() {
        log.info("Admin requesting system metrics");

        List<MetricsResponse> metrics = messageService.getAllUserMetrics();

        log.info("Returning metrics for {} users", metrics.size());
        return ResponseEntity.ok(metrics);
    }

}
