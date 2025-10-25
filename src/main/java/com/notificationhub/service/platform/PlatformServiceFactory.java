package com.notificationhub.service.platform;

import com.notificationhub.enums.PlatformType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PlatformServiceFactory {
    private final Map<PlatformType, PlatformService> services;

    public PlatformServiceFactory(List<PlatformService> platformServices) {
        this.services = new HashMap<>();

        for (PlatformService service : platformServices) {
            services.put(service.getPlatformType(), service);
        }
    }

    /**
     * Obtiene el servicio para una plataforma específica
     *
     * @param platformType Tipo de plataforma
     * @return PlatformService correspondiente
     * @throws IllegalArgumentException si la plataforma no está soportada
     */
    public PlatformService getService(PlatformType platformType) {
        PlatformService service = services.get(platformType);

        if (service == null) {
            throw new IllegalArgumentException("Platform not supported: " + platformType);
        }

        if (!service.isConfigured()) {
            throw new IllegalStateException("Platform not configured: " + platformType);
        }

        return service;
    }

    /**
     * Verifica si una plataforma está disponible y configurada
     *
     * @param platformType Tipo de plataforma
     * @return true si está disponible, false en caso contrario
     */
    public boolean isPlatformAvailable(PlatformType platformType) {
        PlatformService service = services.get(platformType);
        return service != null && service.isConfigured();
    }
}
