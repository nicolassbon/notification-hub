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
}