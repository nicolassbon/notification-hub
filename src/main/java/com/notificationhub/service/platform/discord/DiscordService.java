package com.notificationhub.service.platform.discord;

import com.notificationhub.entity.MessageDelivery;
import com.notificationhub.enums.DeliveryStatus;
import com.notificationhub.enums.PlatformType;
import com.notificationhub.exception.MessageDeliveryException;
import com.notificationhub.service.platform.PlatformService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class DiscordService implements PlatformService {

    private final WebClient webClient;
    private final String webhookUrl;

    public DiscordService(
            WebClient.Builder webClientBuilder,
            @Value("${discord.webhook.url}") String webhookUrl) {

        this.webhookUrl = webhookUrl;
        this.webClient = webClientBuilder.build();
    }

    @Override
    public MessageDelivery send(String content, String destination, String username) {
        // Discord webhooks no usan destination, siempre van al canal configurado
        // Pero guardamos la URL como destination para tracking
        String finalDestination = (destination != null && !destination.isEmpty()) ? destination : webhookUrl;

        log.info("Sending message to Discord webhook");

        MessageDelivery delivery = MessageDelivery.builder()
                .platformType(PlatformType.DISCORD)
                .destination(finalDestination)
                .status(DeliveryStatus.PENDING)
                .build();

        try {
            // Preparar request body para Discord
            Map<String, Object> requestBody = new HashMap<>();

            String signedContent = String.format("📨 **From: %s**\n\n%s", username, content);
            requestBody.put("content", signedContent);
            requestBody.put("username", "Notification Hub Bot");

            // Llamar a Discord Webhook
            webClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            response -> {
                                log.error("Discord webhook returned error status: {}", response.statusCode());
                                return response.bodyToMono(String.class)
                                        .flatMap(body -> Mono.error(
                                                new MessageDeliveryException("Discord webhook error: " + body)
                                        ));
                            }
                    )
                    .toBodilessEntity()
                    .doOnSuccess(response -> {
                        log.info("Message sent successfully to Discord. Status: {}", response.getStatusCode());
                    })
                    .block();

            // Discord retorna 204 No Content en éxito
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("status", "success");
            responseData.put("timestamp", LocalDateTime.now().toString());

            delivery.markAsSuccess(responseData);

        } catch (Exception e) {
            log.error("Exception sending message to Discord: {}", e.getMessage(), e);
            delivery.markAsFailed("Exception: " + e.getMessage());
        }

        return delivery;
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.DISCORD;
    }

    @Override
    public boolean isConfigured() {
        return webhookUrl != null && !webhookUrl.isEmpty()
                && webhookUrl.startsWith("https://discord.com/api/webhooks/");
    }
}
