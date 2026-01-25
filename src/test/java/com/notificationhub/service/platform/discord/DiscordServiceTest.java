package com.notificationhub.service.platform.discord;

import com.notificationhub.enums.PlatformType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiscordService Unit Tests")
public class DiscordServiceTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    private DiscordService discordService;

    @BeforeEach
    void setUp() {
        when(webClientBuilder.build()).thenReturn(null);

        String webhookUrl = "https://discord.com/api/webhooks/123456789/abcdefghijklmnop";
        discordService = new DiscordService(webClientBuilder, webhookUrl);
    }

    @Test
    @DisplayName("Should return correct platform type")
    void getPlatformTypeReturnsDiscord() {
        assertEquals(PlatformType.DISCORD, discordService.getPlatformType());
    }

    @Test
    @DisplayName("Should be configured when webhook URL is valid")
    void isConfiguredReturnsTrueWhenProperlyConfigured() {
        assertTrue(discordService.isConfigured());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "http://invalid-url.com")
    @DisplayName("Should return false when webhook URL is not configured")
    void isConfiguredReturnsFalse(String url) {

        DiscordService unconfiguredService = new DiscordService(webClientBuilder, url);

        assertFalse(unconfiguredService.isConfigured());
    }

    @Test
    @DisplayName("Should be configured with valid Discord webhook URL")
    void isConfiguredReturnsTrueForValidDiscordUrl() {
        String validUrl = "https://discord.com/api/webhooks/987654321/zyxwvutsrqponmlk";
        DiscordService validService = new DiscordService(webClientBuilder, validUrl);
        assertTrue(validService.isConfigured());
    }

    @Test
    @DisplayName("Should not be configured with discordapp.com webhook URL")
    void isConfiguredReturnsFalseForDiscordAppUrl() {
        String oldFormatUrl = "https://discordapp.com/api/webhooks/123/abc";
        DiscordService oldService = new DiscordService(webClientBuilder, oldFormatUrl);
        assertFalse(oldService.isConfigured());
    }
}
