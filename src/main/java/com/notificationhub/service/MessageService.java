package com.notificationhub.service;

import com.notificationhub.dto.request.MessageRequest;
import com.notificationhub.dto.response.MetricsResponse;
import com.notificationhub.entity.Message;
import com.notificationhub.enums.DeliveryStatus;
import com.notificationhub.enums.PlatformType;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageService {

    /**
     * Envia un mensaje a multiples plataformas
     *
     * @param request Detalles del mensaje y destinos
     * @return Mensaje enviado con detalles de entrega
     * Puede lanzar RateLimitExceededException
     * Si las dos son exitosas, cuenta como 1 solo mensaje
     */
    Message sendMessage(MessageRequest request);

    /**
     * Obtiene todos los mensajes del sistema (ADMIN)
     *
     * @return Lista de todos los mensajes
     */
    List<Message> getAllMessages();

    /**
     * Obtiene mensajes del usuario con filtros
     *
     * @param status   Filtro por estado (opcional)
     * @param platform Filtro por plataforma (opcional)
     * @param from     Fecha desde (opcional)
     * @param to       Fecha hasta (opcional)
     * @return Lista de mensajes filtrados
     */
    List<Message> getUserMessagesWithFilters(
            DeliveryStatus status,
            PlatformType platform,
            LocalDateTime from,
            LocalDateTime to);

    /**
     * Obtiene métricas de los usuarios (ADMIN)
     *
     * @return Lista de métricas por usuario
     */
    List<MetricsResponse> getAllUserMetrics();
}
