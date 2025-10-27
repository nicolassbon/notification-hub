package com.notificationhub.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Message send request with content and destinations")
public class MessageRequest {
    @NotBlank(message = "Message content is required")
    @Size(max = 4000, message = "Message content must not exceed 4000 characters")
    @Schema(description = "Message content", example = "Hello from Notification Hub!", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 4000)
    private String content;


    @NotEmpty(message = "At least one destination is required")
    @Valid
    @Schema(description = "List of message destinations", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<DestinationRequest> destinations;
}
