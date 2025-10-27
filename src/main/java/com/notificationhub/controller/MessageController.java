package com.notificationhub.controller;

import com.notificationhub.dto.request.MessageRequest;
import com.notificationhub.dto.response.ErrorResponse;
import com.notificationhub.dto.response.MessageResponse;
import com.notificationhub.entity.Message;
import com.notificationhub.enums.DeliveryStatus;
import com.notificationhub.enums.PlatformType;
import com.notificationhub.mapper.MessageMapper;
import com.notificationhub.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/messages")
@Tag(name = "Messages", description = "Send and manage messages across multiple platforms (Telegram, Discord)")
@Slf4j
public class MessageController {
    private final MessageService messageService;
    private final MessageMapper messageMapper;

    public MessageController(MessageService messageService, MessageMapper messageMapper) {
        this.messageService = messageService;
        this.messageMapper = messageMapper;
    }

    @PostMapping("/send")
    @Operation(
            summary = "Send message to multiple platforms",
            description = """
                    Send a message to one or more platforms (Telegram, Discord).
                    Messages are signed with the username of the requesting user.
                    
                    **Delivery Behavior:**
                    - Message is always persisted in the database
                    - Each platform delivery is attempted independently
                    - A message is considered sent if at least one delivery succeeds
                    - Failed deliveries include detailed error messages
                    - Rate limit: 100 messages per day (configurable per user)
                    """,
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Message sent successfully (at least one delivery succeeded)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "success",
                                            summary = "All deliveries successful",
                                            value = """
                                                    {
                                                      "id": 1,
                                                      "content": "Hello from Notification Hub!",
                                                      "username": "admin",
                                                      "createdAt": "2025-10-27T10:49:28.5073736",
                                                      "deliveries": [
                                                        {
                                                          "id": 1,
                                                          "platform": "TELEGRAM",
                                                          "destination": "123456789",
                                                          "status": "SUCCESS",
                                                          "providerResponse": {
                                                            "message_id": 123
                                                          },
                                                          "sentAt": 2025-10-27T10:49:28.5073736"
                                                        }
                                                      ]
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "partial_failure",
                                            summary = "Some deliveries failed",
                                            value = """
                                                    {
                                                      "id": 5,
                                                      "content": "Deberia fallar test",
                                                      "username": "testuser",
                                                      "createdAt": "2025-10-27T10:49:28.5073736",
                                                      "deliveries": [
                                                        {
                                                          "id": 6,
                                                          "platform": "TELEGRAM",
                                                          "destination": "123145",
                                                          "status": "FAILED",
                                                          "providerResponse": null,
                                                          "errorMessage": "Exception: Failed to send message to Telegram: 400 Bad Request from POST https://api.telegram.org/bot8479729496:AAHPMD4UcPy3h5YMeXPL6gNlCpZGtDndLhw/sendMessage",
                                                          "sentAt": "2025-10-27T10:49:28.5073736"
                                                        }
                                                      ]
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data or message content",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "status": 400,
                                              "error": "Validation Failed",
                                              "message": "Invalid request data",
                                              "timestamp": "2025-10-27T10:41:34.3468297",
                                              "path": "/api/messages/send",
                                              "details": [
                                                "content: Message content is required"
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated - JWT token required",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Invalid or expired JWT token",
                                              "timestamp": "2025-10-27T10:41:55.7123479",
                                              "path": "/api/messages/send"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Daily message limit exceeded",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "status": 429,
                                              "error": "Too Many Requests",
                                              "message": "Daily message limit exceeded. Remaining: 0",
                                              "timestamp": "2025-10-27T10:41:55.7123479",
                                              "path": "/api/messages/send"
                                            }
                                            """
                            )
                    )
            )
    })
    public ResponseEntity<MessageResponse> sendMessage(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Message content and destination platforms",
                    required = true,
                    content = @Content(schema = @Schema(implementation = MessageRequest.class))
            )
            @Valid @RequestBody MessageRequest request) {
        log.info("Received message send request for {} destinations", request.getDestinations().size());

        Message message = messageService.sendMessage(request);
        MessageResponse response = messageMapper.toResponse(message);

        log.info("Message sent successfully. ID: {}", message.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(
            summary = "Get my messages",
            description = "Retrieve all messages sent by the authenticated user. " +
                    "Supports optional filtering by delivery status, platform, and date range. " +
                    "Without filters, returns all messages ordered by creation date (newest first).",
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
                    description = "Not authenticated - JWT token required",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Invalid or expired JWT token",
                                              "timestamp": "2025-10-27T10:41:55.7123479",
                                              "path": "/api/messages"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid filter parameters",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "status": 400,
                                              "error": "Bad Request",
                                              "message": "Invalid date format for parameter 'from'",
                                              "timestamp": "2025-10-27T10:41:34.3468297",
                                              "path": "/api/messages",
                                              "details": [
                                                "from: must be a valid ISO 8601 date time"
                                              ]
                                            }
                                            """
                            )
                    )
            )
    })
    public ResponseEntity<List<MessageResponse>> getMyMessages(
            @Parameter(
                    description = "Filter by delivery status (SUCCESS, PENDING, FAILED)",
                    example = "SUCCESS"
            )
            @RequestParam(required = false) DeliveryStatus status,

            @Parameter(
                    description = "Filter by platform (TELEGRAM, DISCORD)",
                    example = "TELEGRAM"
            )
            @RequestParam(required = false) PlatformType platform,

            @Parameter(
                    description = "Start date for date range filter (ISO 8601 format)",
                    example = "2025-01-01T00:00:00"
            )
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,

            @Parameter(
                    description = "End date for date range filter (ISO 8601 format)",
                    example = "2025-12-31T23:59:59"
            )
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        log.info("User requesting messages with filters - status: {}, platform: {}, from: {}, to: {}",
                status, platform, from, to);

        List<Message> messages = messageService.getUserMessagesWithFilters(status, platform, from, to);
        List<MessageResponse> responses = messageMapper.toResponseList(messages);

        log.info("Returning {} messages to user", responses.size());
        return ResponseEntity.ok(responses);
    }

}
