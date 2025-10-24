package com.notificationhub.dto.request;

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
public class MessageRequest {
    @NotBlank(message = "Message content is required")
    @Size(max = 4000, message = "Message content must not exceed 4000 characters")
    private String content;

    @NotEmpty(message = "At least one destination is required")
    @Valid
    private List<DestinationRequest> destinations;
}
