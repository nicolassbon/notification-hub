package com.notificationhub.service.impl;

import com.notificationhub.dto.criteria.MessageFilterCriteria;
import com.notificationhub.dto.request.DestinationRequest;
import com.notificationhub.dto.request.MessageRequest;
import com.notificationhub.dto.response.MetricsResponse;
import com.notificationhub.entity.DailyMessageCount;
import com.notificationhub.entity.Message;
import com.notificationhub.entity.MessageDelivery;
import com.notificationhub.entity.User;
import com.notificationhub.enums.DeliveryStatus;
import com.notificationhub.enums.PlatformType;
import com.notificationhub.repository.DailyMessageCountRepository;
import com.notificationhub.repository.MessageDeliveryRepository;
import com.notificationhub.repository.MessageRepository;
import com.notificationhub.repository.UserRepository;
import com.notificationhub.service.MessageService;
import com.notificationhub.service.RateLimitService;
import com.notificationhub.service.platform.PlatformService;
import com.notificationhub.service.platform.PlatformServiceFactory;
import com.notificationhub.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final PlatformServiceFactory platformServiceFactory;
    private final UserRepository userRepository;
    private final DailyMessageCountRepository dailyMessageCountRepository;
    private final RateLimitService rateLimitService;
    private final SecurityUtils securityUtils;
    private final MessageDeliveryRepository messageDeliveryRepository;

    public MessageServiceImpl(MessageRepository messageRepository,
                              PlatformServiceFactory platformServiceFactory,
                              UserRepository userRepository,
                              DailyMessageCountRepository dailyMessageCountRepository,
                              RateLimitService rateLimitService,
                              SecurityUtils securityUtils,
                              MessageDeliveryRepository messageDeliveryRepository) {
        this.messageRepository = messageRepository;
        this.platformServiceFactory = platformServiceFactory;
        this.userRepository = userRepository;
        this.dailyMessageCountRepository = dailyMessageCountRepository;
        this.rateLimitService = rateLimitService;
        this.securityUtils = securityUtils;
        this.messageDeliveryRepository = messageDeliveryRepository;
    }

    public Message sendMessage(MessageRequest request) {
        log.info("Processing message request with {} destinations", request.getDestinations().size());

        User currentUser = getAuthenticatedUser();
        log.info("User {} is sending a message", currentUser.getUsername());

        rateLimitService.checkRateLimit(currentUser);

        Message message = Message.builder()
                .user(currentUser)
                .content(request.getContent())
                .build();
        log.debug("Created message entity for user {}", currentUser.getUsername());

        List<MessageDelivery> deliveries = processMessageDeliveries(request, message);

        Message savedMessage = messageRepository.save(message);
        log.info("Message saved with {} deliveries", deliveries.size());

        updateRateLimitIfNeeded(currentUser, deliveries);

        logMessageCompletion(savedMessage, deliveries);

        return savedMessage;
    }

    public List<Message> getAllMessages() {
        if (!securityUtils.isAdmin()) {
            throw new IllegalStateException("Only admins can view all messages");
        }

        List<Message> messages = messageRepository.findAll();

        log.info("Admin retrieved {} messages", messages.size());

        return messages;
    }

    public List<Message> getUserMessagesWithFilters(
            DeliveryStatus status,
            PlatformType platform,
            LocalDateTime from,
            LocalDateTime to) {

        User currentUser = getAuthenticatedUser();

        MessageFilterCriteria criteria = MessageFilterCriteria.builder()
                .user(currentUser)
                .status(status)
                .platform(platform)
                .from(from)
                .to(to)
                .build();

        List<Message> messages = retrieveUserMessages(currentUser, criteria);
        List<Message> filteredMessages = messageDeliveryRepository.findMessagesByFilters(criteria);

        log.info("Retrieved {} filtered messages for user {}", filteredMessages.size(), currentUser.getUsername());
        return filteredMessages;
    }

    public List<MetricsResponse> getAllUserMetrics() {
        if (!securityUtils.isAdmin()) {
            throw new IllegalStateException("Only admins can view metrics");
        }

        List<User> users = userRepository.findAll();
        LocalDate today = LocalDate.now();

        return users.stream()
                .map(user -> {
                    long totalMessages = messageRepository.countByUser(user);

                    DailyMessageCount todayCount = dailyMessageCountRepository
                            .findByUserAndDate(user, today)
                            .orElse(null);

                    int messagesSentToday = todayCount != null ? todayCount.getCount() : 0;
                    int remainingToday = user.getDailyMessageLimit() - messagesSentToday;

                    return MetricsResponse.builder()
                            .username(user.getUsername())
                            .totalMessagesSent(totalMessages)
                            .messagesSentToday(messagesSentToday)
                            .remainingMessagesToday(Math.max(0, remainingToday))
                            .dailyLimit(user.getDailyMessageLimit())
                            .build();
                })
                .collect(Collectors.toList());
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
            return messageRepository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(user, criteria.from(), criteria.to());
        } else {
            return messageRepository.findByUserOrderByCreatedAtDesc(user);
        }
    }
}
