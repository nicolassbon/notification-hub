package com.notificationhub.service;

import com.notificationhub.dto.criteria.MessageFilterCriteria;
import com.notificationhub.dto.request.DestinationRequest;
import com.notificationhub.dto.request.MessageRequest;
import com.notificationhub.entity.Message;
import com.notificationhub.entity.MessageDelivery;
import com.notificationhub.entity.User;
import com.notificationhub.enums.DeliveryStatus;
import com.notificationhub.enums.PlatformType;
import com.notificationhub.repository.MessageRepository;
import com.notificationhub.service.platform.PlatformService;
import com.notificationhub.service.platform.PlatformServiceFactory;
import com.notificationhub.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@Transactional
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final PlatformServiceFactory platformServiceFactory;
    private final RateLimitService rateLimitService;
    private final SecurityUtils securityUtils;

    public MessageServiceImpl(MessageRepository messageRepository,
                              PlatformServiceFactory platformServiceFactory,
                              RateLimitService rateLimitService,
                              SecurityUtils securityUtils) {
        this.messageRepository = messageRepository;
        this.platformServiceFactory = platformServiceFactory;
        this.rateLimitService = rateLimitService;
        this.securityUtils = securityUtils;
    }

    /**
     * Envia un mensaje a multiples plataformas
     */
    public Message sendMessage(MessageRequest request) {
        log.info("Processing message request with {} destinations", request.getDestinations().size());

        User currentUser = getAuthenticatedUser();
        log.info("User {} is sending a message", currentUser.getUsername());

        // Puede lanzar RateLimitExceededException
        rateLimitService.checkRateLimit(currentUser);

        Message message = Message.builder()
                .user(currentUser)
                .content(request.getContent())
                .build();
        log.debug("Created message entity for user {}", currentUser.getUsername());

        List<MessageDelivery> deliveries = processMessageDeliveries(request, message);

        Message savedMessage = messageRepository.save(message);
        log.info("Message saved with {} deliveries", deliveries.size());

        // Si las dos son exitosas, cuenta como 1 solo mensaje
        updateRateLimitIfNeeded(currentUser, deliveries);

        logMessageCompletion(savedMessage, deliveries);

        return savedMessage;
    }

    /**
     * Obtiene todos los mensajes del sistema (ADMIN)
     *
     * @return Lista de todos los mensajes
     */
    public List<Message> getAllMessages() {
        if (!securityUtils.isAdmin()) {
            throw new IllegalStateException("Only admins can view all messages");
        }

        List<Message> messages = messageRepository.findAll();

        log.info("Admin retrieved {} messages", messages.size());

        return messages;
    }

    /**
     * Obtiene mensajes del usuario con filtros
     *
     * @param status   Filtro por estado (opcional)
     * @param platform Filtro por plataforma (opcional)
     * @param from     Fecha desde (opcional)
     * @param to       Fecha hasta (opcional)
     * @return Lista de mensajes filtrados
     */
    public List<Message> getUserMessagesWithFilters(
            DeliveryStatus status,
            PlatformType platform,
            LocalDateTime from,
            LocalDateTime to) {

        User currentUser = getAuthenticatedUser();

        MessageFilterCriteria criteria = MessageFilterCriteria.builder()
                .status(status)
                .platform(platform)
                .from(from)
                .to(to)
                .build();

        List<Message> messages = retrieveUserMessages(currentUser, criteria);
        List<Message> filteredMessages = applyFilters(messages, criteria);

        log.info("Retrieved {} filtered messages for user {}", filteredMessages.size(), currentUser.getUsername());
        return filteredMessages;
    }

    private User getAuthenticatedUser() {
        User currentUser = securityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("No authenticated user found");
        }
        return currentUser;
    }

    private List<MessageDelivery> processMessageDeliveries(MessageRequest request, Message message) {
        List<MessageDelivery> deliveries = new ArrayList<>();

        for (DestinationRequest destination : request.getDestinations()) {
            MessageDelivery delivery = processSingleDelivery(destination, message);
            deliveries.add(delivery);
        }

        return deliveries;
    }

    private MessageDelivery processSingleDelivery(DestinationRequest destination, Message message) {
        try {
            log.info("Sending message to platform: {}", destination.getPlatform());

            PlatformService platformService = platformServiceFactory.getService(destination.getPlatform());
            MessageDelivery delivery = platformService.send(
                    message.getContent(),
                    destination.getDestination(),
                    message.getUser().getUsername()
            );

            message.addDelivery(delivery);
            log.info("Message sent to {}. Status: {}", destination.getPlatform(), delivery.getStatus());

            return delivery;

        } catch (Exception e) {
            log.error("Failed to send message to {}: {}", destination.getPlatform(), e.getMessage());
            return MessageDelivery.builder()
                    .platformType(destination.getPlatform())
                    .destination(destination.getDestination())
                    .status(DeliveryStatus.FAILED)
                    .errorMessage("Exception: " + e.getMessage())
                    .build();
        }
    }

    private void updateRateLimitIfNeeded(User user, List<MessageDelivery> deliveries) {
        boolean hasSuccessfulDelivery = deliveries.stream()
                .anyMatch(d -> d.getStatus() == DeliveryStatus.SUCCESS);

        if (hasSuccessfulDelivery) {
            rateLimitService.incrementCounter(user);
            log.info("Rate limit counter incremented for user {}", user.getUsername());
        } else {
            log.warn("No successful deliveries. Rate limit NOT incremented for user {}", user.getUsername());
        }
    }

    private void logMessageCompletion(Message message, List<MessageDelivery> deliveries) {
        long successfulCount = deliveries.stream()
                .filter(d -> d.getStatus() == DeliveryStatus.SUCCESS)
                .count();

        log.info("Message processing completed. ID: {}, Successful deliveries: {}/{}",
                message.getId(), successfulCount, deliveries.size());
    }

    private List<Message> retrieveUserMessages(User user, MessageFilterCriteria criteria) {
        if (criteria.hasDateFilters()) {
            return messageRepository.findByUserAndDateRange(user, criteria.from(), criteria.to());
        } else {
            return messageRepository.findMessagesByUser(user);
        }
    }

    private List<Message> applyFilters(List<Message> messages, MessageFilterCriteria criteria) {
        if (!criteria.hasFilters()) {
            return messages;
        }

        return messages.stream()
                .filter(message -> matchesStatus(message, criteria.status()))
                .filter(message -> matchesPlatform(message, criteria.platform()))
                .toList();
    }

    private boolean matchesStatus(Message message, DeliveryStatus status) {
        if (status == null) return true;

        return message.getDeliveries().stream()
                .anyMatch(d -> d.getStatus() == status);
    }

    private boolean matchesPlatform(Message message, PlatformType platform) {
        if (platform == null) return true;

        return message.getDeliveries().stream()
                .anyMatch(d -> d.getPlatformType() == platform);
    }

}
