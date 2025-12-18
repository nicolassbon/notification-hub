package com.notificationhub.service;

import com.notificationhub.dto.request.MessageRequest;
import com.notificationhub.dto.response.MetricsResponse;
import com.notificationhub.entity.Message;
import com.notificationhub.enums.DeliveryStatus;
import com.notificationhub.enums.PlatformType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
     * Obtiene todos los mensajes con paginación (ADMIN)
     *
     * @param pageable Parámetros de paginación
     * @return Página de mensajes
     */
    Page<Message> getAllMessages(Pageable pageable);

    /**
     * Obtiene mensajes del usuario con filtros y paginación
     *
     * @param status   Filtro por estado de entrega (opcional)
     * @param platform Filtro por tipo de plataforma (opcional)
     * @param from     Filtro de fecha desde (opcional)
     * @param to       Filtro de fecha hasta (opcional)
     * @param pageable Parámetros de paginación
     * @return Página de mensajes que cumplen con los filtros
     */
    Page<Message> getUserMessagesWithFilters(
            DeliveryStatus status,
            PlatformType platform,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable);

    /**
     * Obtiene métricas de los usuarios (ADMIN)
     *
     * @return Lista de métricas por usuario
     */
    List<MetricsResponse> getAllUserMetrics();
}
