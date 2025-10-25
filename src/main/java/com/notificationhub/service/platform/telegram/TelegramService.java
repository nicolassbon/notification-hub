package com.notificationhub.service.platform.telegram;

import com.notificationhub.entity.MessageDelivery;
import com.notificationhub.enums.DeliveryStatus;
import com.notificationhub.enums.PlatformType;
import com.notificationhub.exception.MessageDeliveryException;
import com.notificationhub.service.platform.PlatformService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class TelegramService implements PlatformService {

    private final WebClient webClient;
    private final String botToken;
    private final String defaultChatId;
    private final String TELEGRAM_API_URL = "https://api.telegram.org/bot";

    public TelegramService(
            WebClient.Builder webClientBuilder,
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.default-chat-id}") String defaultChatId) {

        this.botToken = botToken;
        this.defaultChatId = defaultChatId;
        this.webClient = webClientBuilder
                .baseUrl(TELEGRAM_API_URL + botToken)
                .build();
    }

    public MessageDelivery send(String content, String destination, String username) {
        String chatId = (destination != null && !destination.isEmpty()) ? destination : defaultChatId;

        log.info("Sending message to Telegram. Chat ID: {}", chatId);

        MessageDelivery delivery = MessageDelivery.builder()
                .platformType(PlatformType.TELEGRAM)
                .destination(chatId)
                .status(DeliveryStatus.PENDING)
                .build();

        try {
            // Preparar request body para Telegram
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("chat_id", chatId);

            String signedContent = String.format("📨 *From: %s*\n\n%s", username, content);
            requestBody.put("text", signedContent);
            requestBody.put("parse_mode", "markdown");

            // Llamar a Telegram API
            var response = webClient.post()
                    .uri("/sendMessage")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .onErrorResume(e -> {
                        log.error("Error calling Telegram API: {}", e.getMessage());
                        return Mono.error(new MessageDeliveryException("Failed to send message to Telegram: " + e.getMessage()));
                    })
                    .block();

            // Verificar respuesta
            if (response != null && Boolean.TRUE.equals(response.get("ok"))) {
                log.info("Message sent successfully to Telegram");
                delivery.markAsSuccess(response);
            } else {
                String errorMsg = response != null ? response.toString() : "Unknown error";
                log.error("Telegram API returned error: {}", errorMsg);
                delivery.markAsFailed("Telegram API error: " + errorMsg);
            }

        } catch (Exception e) {
            log.error("Exception sending message to Telegram: {}", e.getMessage(), e);
            delivery.markAsFailed("Exception: " + e.getMessage());
        }

        return delivery;
    }

    public PlatformType getPlatformType() {
        return PlatformType.TELEGRAM;
    }

    public boolean isConfigured() {
        return botToken != null && !botToken.isEmpty()
                && defaultChatId != null && !defaultChatId.isEmpty();
    }
}
