package com.notificationhub.service.platform.telegram;

import com.notificationhub.enums.PlatformType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TelegramService Unit Tests - Simplified")
public class TelegramServiceTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    private TelegramService telegramService;

    private final String botToken = "test-bot-token";
    private final String defaultChatId = "-123456789";

    @BeforeEach
    void setUp() {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);

        telegramService = new TelegramService(webClientBuilder, botToken, defaultChatId);
    }

    @Test
    @DisplayName("Should return correct platform type")
    void getPlatformTypeReturnsTelegram() {
        assertEquals(PlatformType.TELEGRAM, telegramService.getPlatformType());
    }

    @Test
    @DisplayName("Should be configured when token and chat ID are provided")
    void isConfiguredReturnsTrueWhenProperlyConfigured() {
        assertTrue(telegramService.isConfigured());
    }

    @Test
    @DisplayName("Should not be configured when token is empty")
    void isConfiguredReturnsFalseWhenTokenEmpty() {
        TelegramService unconfiguredService = new TelegramService(webClientBuilder, "", defaultChatId);
        assertFalse(unconfiguredService.isConfigured());
    }

    @Test
    @DisplayName("Should not be configured when token is null")
    void isConfiguredReturnsFalseWhenTokenNull() {
        TelegramService unconfiguredService = new TelegramService(webClientBuilder, null, defaultChatId);
        assertFalse(unconfiguredService.isConfigured());
    }

    @Test
    @DisplayName("Should not be configured when chat ID is empty")
    void isConfiguredReturnsFalseWhenChatIdEmpty() {
        TelegramService unconfiguredService = new TelegramService(webClientBuilder, botToken, "");
        assertFalse(unconfiguredService.isConfigured());
    }

    @Test
    @DisplayName("Should not be configured when chat ID is null")
    void isConfiguredReturnsFalseWhenChatIdNull() {
        TelegramService unconfiguredService = new TelegramService(webClientBuilder, botToken, null);
        assertFalse(unconfiguredService.isConfigured());
    }
}