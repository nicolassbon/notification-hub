package com.notificationhub.service.platform.telegram;

import com.notificationhub.enums.DeliveryStatus;
import com.notificationhub.enums.PlatformType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TelegramService Unit Tests - Simplified")
public class TelegramServiceTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private TelegramService telegramService;

    private final String botToken = "test-bot-token";
    private final String defaultChatId = "-123456789";

    @BeforeEach
    void setUp() {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        telegramService = new TelegramService(webClientBuilder, botToken, defaultChatId);
    }

    @Test
    @DisplayName("Should return correct platform type")
    void getPlatformTypeReturnsTelegram() {
        assertThat(telegramService.getPlatformType()).isEqualTo(PlatformType.TELEGRAM);
    }

    @Test
    @DisplayName("Should be configured when token and chat ID are provided")
    void isConfiguredReturnsTrueWhenProperlyConfigured() {
        assertThat(telegramService.isConfigured()).isTrue();
    }

    @Test
    @DisplayName("Should not be configured when token is empty")
    void isConfiguredReturnsFalseWhenTokenEmpty() {
        TelegramService unconfiguredService = new TelegramService(webClientBuilder, "", defaultChatId);

        assertThat(unconfiguredService.isConfigured()).isFalse();
    }

    @Test
    @DisplayName("Should not be configured when token is null")
    void isConfiguredReturnsFalseWhenTokenNull() {
        TelegramService unconfiguredService = new TelegramService(webClientBuilder, null, defaultChatId);

        assertThat(unconfiguredService.isConfigured()).isFalse();
    }

    @Test
    @DisplayName("Should not be configured when chat ID is empty")
    void isConfiguredReturnsFalseWhenChatIdEmpty() {
        TelegramService unconfiguredService = new TelegramService(webClientBuilder, botToken, "");

        assertThat(unconfiguredService.isConfigured()).isFalse();
    }

    @Test
    @DisplayName("Should not be configured when chat ID is null")
    void isConfiguredReturnsFalseWhenChatIdNull() {
        TelegramService unconfiguredService = new TelegramService(webClientBuilder, botToken, null);

        assertThat(unconfiguredService.isConfigured()).isFalse();
    }

    @Test
    @DisplayName("Should send message to Telegram default chat when destination is empty")
    void sendsMessageToDefaultChatWhenDestinationIsEmpty() {
        Map<String, Object> response = Map.of("ok", true);
        mockTelegramResponse(response);

        var delivery = telegramService.send("Spring rocks", "", "duke");

        assertThat(delivery.getPlatformType()).isEqualTo(PlatformType.TELEGRAM);
        assertThat(delivery.getDestination()).isEqualTo(defaultChatId);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.SUCCESS);
        assertThat(delivery.getProviderResponse()).isEqualTo(response);
        verify(requestBodySpec).bodyValue(argThat(requestBody -> containsTelegramMessage(requestBody, defaultChatId)));
    }

    @Test
    @DisplayName("Should mark delivery as failed when Telegram API returns an error")
    void marksDeliveryAsFailedWhenTelegramApiReturnsError() {
        mockTelegramResponse(Map.of("ok", false, "description", "chat not found"));

        var delivery = telegramService.send("Spring rocks", "custom-chat", "duke");

        assertThat(delivery.getDestination()).isEqualTo("custom-chat");
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(delivery.getErrorMessage()).contains("Telegram API error");
    }

    private void mockTelegramResponse(Map<String, Object> response) {
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/sendMessage")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(response));
    }

    private boolean containsTelegramMessage(Object requestBody, String chatId) {
        if (!(requestBody instanceof Map<?, ?> body)) {
            return false;
        }
        return chatId.equals(body.get("chat_id"))
                && "**From: duke**\n\nSpring rocks".equals(body.get("text"))
                && "markdown".equals(body.get("parse_mode"));
    }
}
