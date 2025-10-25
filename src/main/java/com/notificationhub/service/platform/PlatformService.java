package com.notificationhub.service.platform;

import com.notificationhub.entity.MessageDelivery;
import com.notificationhub.enums.PlatformType;


public interface PlatformService {

    /**
     * Envía un mensaje a la plataforma
     *
     * @param content     Contenido del mensaje
     * @param destination Destino específico (chat_id, channel_id, etc). Si es null, usa el default.
     * @param username    Firma con el nombre de usuario del remitente
     * @return MessageDelivery con el resultado del envío
     */
    MessageDelivery send(String content, String destination, String username);

    /**
     * Retorna el tipo de plataforma que maneja este servicio
     *
     * @return PlatformType
     */
    PlatformType getPlatformType();

    /**
     * Verifica si la plataforma está configurada y activa
     */
    boolean isConfigured();
}
