package com.notificationhub.service;

import com.notificationhub.entity.User;
import com.notificationhub.exception.custom.RateLimitExceededException;

public interface RateLimitService {
    /**
     * Verifica si el usuario puede enviar un mensaje según su límite diario
     *
     * @param user Usuario a verificar
     * @throws RateLimitExceededException si el usuario alcanzó su límite
     */
    void checkRateLimit(User user);

    /**
     * Incrementa el contador de mensajes del usuario para hoy
     *
     * @param user Usuario que envió el mensaje
     */
    void incrementCounter(User user);

    /**
     * Obtiene cuántos mensajes puede enviar el usuario hoy (Metrics Admin)
     *
     * @param user Usuario
     * @return Cantidad de mensajes restantes
     */
    int getRemainingMessages(User user);
}
