package com.notificationhub.repository;

import com.notificationhub.entity.Message;
import com.notificationhub.entity.MessageDelivery;
import com.notificationhub.entity.User;
import com.notificationhub.enums.DeliveryStatus;
import com.notificationhub.enums.PlatformType;
import com.notificationhub.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("MessageRepository Unit Tests")
class MessageRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MessageRepository messageRepository;

    private User testUser;
    private User anotherUser;
    private Message yesterdayMessage;

    @BeforeEach
    void setUp() {
        entityManager.clear();

        testUser = User.builder()
                .username("testuser")
                .passwordHash("$2a$10$hashedpassword")
                .role(Role.USER)
                .dailyMessageLimit(100)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        anotherUser = User.builder()
                .username("anotheruser")
                .passwordHash("$2a$10$hashedpassword2")
                .role(Role.USER)
                .dailyMessageLimit(100)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        entityManager.persist(testUser);
        entityManager.persist(anotherUser);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        LocalDateTime lastWeek = now.minusDays(7);

        Message todayMessage = Message.builder()
                .user(testUser)
                .content("Today's message")
                .createdAt(now)
                .build();

        yesterdayMessage = Message.builder()
                .user(testUser)
                .content("Yesterday's message")
                .createdAt(yesterday)
                .build();

        Message oldMessage = Message.builder()
                .user(testUser)
                .content("Last week's message")
                .createdAt(lastWeek)
                .build();

        Message otherUserMessage = Message.builder()
                .user(anotherUser)
                .content("Another user's message")
                .createdAt(now)
                .build();

        entityManager.persist(todayMessage);
        entityManager.persist(yesterdayMessage);
        entityManager.persist(oldMessage);
        entityManager.persist(otherUserMessage);
        entityManager.flush();
    }

    @Test
    @DisplayName("Should find all messages by user")
    void findMessagesByUserWhenUserHasMessagesReturnsUserMessages() {
        List<Message> userMessages = messageRepository.findByUserOrderByCreatedAtDesc(testUser);

        assertNotNull(userMessages);
        assertEquals(3, userMessages.size());
        assertTrue(userMessages.stream().allMatch(msg -> msg.getUser().equals(testUser)));
        assertTrue(userMessages.stream().anyMatch(msg -> msg.getContent().equals("Today's message")));
        assertTrue(userMessages.stream().anyMatch(msg -> msg.getContent().equals("Yesterday's message")));
        assertTrue(userMessages.stream().anyMatch(msg -> msg.getContent().equals("Last week's message")));
    }

    @Test
    @DisplayName("Should return empty list when user has no messages")
    void findMessagesByUserWhenUserHasNoMessagesReturnsEmptyList() {
        User newUser = User.builder()
                .username("newuser")
                .passwordHash("$2a$10$hashedpassword3")
                .role(Role.USER)
                .dailyMessageLimit(100)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        entityManager.persist(newUser);
        entityManager.flush();

        List<Message> userMessages = messageRepository.findByUserOrderByCreatedAtDesc(newUser);

        assertNotNull(userMessages);
        assertTrue(userMessages.isEmpty());
    }

    @Test
    @DisplayName("Should find messages by user and date range")
    void findByUserAndDateRangeWithValidRangeReturnsFilteredMessages() {
        LocalDateTime from = LocalDateTime.now().minusDays(2);
        LocalDateTime to = LocalDateTime.now();

        List<Message> messages = messageRepository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(testUser, from, to);

        assertNotNull(messages);
        assertEquals(2, messages.size());
        assertTrue(messages.stream().anyMatch(msg -> msg.getContent().equals("Today's message")));
        assertTrue(messages.stream().anyMatch(msg -> msg.getContent().equals("Yesterday's message")));
        assertFalse(messages.stream().anyMatch(msg -> msg.getContent().equals("Last week's message")));

        assertTrue(messages.get(0).getCreatedAt().isAfter(messages.get(1).getCreatedAt()));
    }

    @Test
    @DisplayName("Should return empty list when no messages in date range")
    void findByUserAndDateRangeNoMessagesInRangeReturnsEmptyList() {
        LocalDateTime from = LocalDateTime.now().minusYears(1);
        LocalDateTime to = LocalDateTime.now().minusYears(1).plusDays(1);

        List<Message> messages = messageRepository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(testUser, from, to);

        assertNotNull(messages);
        assertTrue(messages.isEmpty());
    }

    @Test
    @DisplayName("Should handle exact date boundaries correctly")
    void findByUserAndDateRangeWithExactBoundariesReturnsCorrectMessages() {
        LocalDateTime exactTime = yesterdayMessage.getCreatedAt();
        LocalDateTime from = exactTime.minusMinutes(1);
        LocalDateTime to = exactTime.plusMinutes(1);

        List<Message> messages = messageRepository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(testUser, from, to);

        assertNotNull(messages);
        assertEquals(1, messages.size());
        assertEquals("Yesterday's message", messages.get(0).getContent());
    }

    @Test
    @DisplayName("Should not return messages from other users in date range")
    void findByUserAndDateRangeOnlyReturnsRequestedUserMessages() {
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now().plusDays(1);

        List<Message> messages = messageRepository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(testUser, from, to);

        assertNotNull(messages);
        assertTrue(messages.stream().allMatch(msg -> msg.getUser().equals(testUser)));
        assertFalse(messages.stream().anyMatch(msg -> msg.getUser().equals(anotherUser)));
    }

    @Test
    @DisplayName("Should load deliveries with single query (not N+1)")
    void findByUserWithJoinFecthAvoidNPlusOneProblem() {
        for (int i = 0; i < 5; i++) {
            Message msg = Message.builder()
                    .user(testUser)
                    .content("Message " + i)
                    .createdAt(LocalDateTime.now().minusHours(i))
                    .build();

            msg.addDelivery(MessageDelivery.builder()
                    .platformType(PlatformType.TELEGRAM)
                    .destination("dest" + i)
                    .status(DeliveryStatus.SUCCESS)
                    .build()
            );

            entityManager.persist(msg);
        }

        entityManager.flush();
        entityManager.clear();

        List<Message> messages = messageRepository.findByUserOrderByCreatedAtDesc(testUser);

        assertNotNull(messages);
        assertEquals(8, messages.size());

        long messagesWithDeliveries = messages.stream()
                .filter(msg -> !msg.getDeliveries().isEmpty())
                .count();

        assertEquals(5, messagesWithDeliveries, "Should have 5 messages with deliveries");
    }

}