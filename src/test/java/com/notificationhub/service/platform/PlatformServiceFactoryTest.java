package com.notificationhub.service.platform;

import com.notificationhub.enums.PlatformType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlatformServiceFactory Unit Tests")
public class PlatformServiceFactoryTest {

    @Mock
    private PlatformService discordService;

    @Mock
    private PlatformService telegramService;

    private PlatformServiceFactory factory;

    @BeforeEach
    void setUp() {
        lenient().when(discordService.getPlatformType()).thenReturn(PlatformType.DISCORD);
        lenient().when(telegramService.getPlatformType()).thenReturn(PlatformType.TELEGRAM);

        List<PlatformService> services = List.of(discordService, telegramService);

        factory = new PlatformServiceFactory(services);
    }

    @Test
    @DisplayName("Should return service when platform is supported and configured")
    void getServiceSupportedAndConfiguredReturnsService() {
        when(discordService.isConfigured()).thenReturn(true);

        PlatformService result = factory.getService(PlatformType.DISCORD);

        assertNotNull(result);
        assertEquals(discordService, result);
        assertEquals(PlatformType.DISCORD, result.getPlatformType());

        verify(discordService).isConfigured();
    }

    @Test
    @DisplayName("Should throw IllegalStateException when platform exists but is not configured")
    void getServiceNotConfiguredThrowsException() {
        when(telegramService.isConfigured()).thenReturn(false);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> factory.getService(PlatformType.TELEGRAM)
        );

        assertEquals("Platform not configured: TELEGRAM", exception.getMessage());
        verify(telegramService).isConfigured();
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when platform is not supported")
    void getServiceNotSupportedThrowsException() {
        List<PlatformService> partialServices = List.of(discordService);

        PlatformServiceFactory partialFactory = new PlatformServiceFactory(partialServices);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> partialFactory.getService(PlatformType.TELEGRAM)
        );

        assertEquals("Platform not supported: TELEGRAM", exception.getMessage());
    }

    @Test
    @DisplayName("Should handle empty service list gracefully")
    void constructorEmptyListHandlesGracefully() {
        PlatformServiceFactory emptyFactory = new PlatformServiceFactory(Collections.emptyList());

        assertThrows(
                IllegalArgumentException.class,
                () -> emptyFactory.getService(PlatformType.DISCORD)
        );
    }
}
