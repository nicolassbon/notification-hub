package com.notificationhub.repository;

import com.notificationhub.dto.criteria.MessageFilterCriteria;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("MessageDeliveryRepository Unit Tests")
@Transactional
class MessageDeliveryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MessageDeliveryRepository messageDeliveryRepository;

    private User testUser;
    private User anotherUser;
    private Message messageOtherUser;

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

        // Mensaje 1: Hoy con deliveries a Discord (SUCCESS) y Telegram (SUCCESS)
        Message message1 = Message.builder()
                .user(testUser)
                .content("Today's message - both platforms success")
                .createdAt(now)
                .build();

        MessageDelivery delivery1Discord = MessageDelivery.builder()
                .platformType(PlatformType.DISCORD)
                .destination("discord-channel-1")
                .status(DeliveryStatus.SUCCESS)
                .sentAt(now)
                .build();
        message1.addDelivery(delivery1Discord);

        MessageDelivery delivery1Telegram = MessageDelivery.builder()
                .platformType(PlatformType.TELEGRAM)
                .destination("123456789")
                .status(DeliveryStatus.SUCCESS)
                .sentAt(now)
                .build();
        message1.addDelivery(delivery1Telegram);

        entityManager.persist(message1);
        entityManager.flush();

        // Mensaje 2: Ayer con delivery a Discord (FAILED) y Telegram (SUCCESS)
        Message message2 = Message.builder()
                .user(testUser)
                .content("Yesterday's message - mixed status")
                .createdAt(yesterday)
                .build();

        MessageDelivery delivery2Discord = MessageDelivery.builder()
                .platformType(PlatformType.DISCORD)
                .destination("discord-channel-2")
                .status(DeliveryStatus.FAILED)
                .errorMessage("Discord API error")
                .sentAt(yesterday)
                .build();
        message2.addDelivery(delivery2Discord);

        MessageDelivery delivery2Telegram = MessageDelivery.builder()
                .platformType(PlatformType.TELEGRAM)
                .destination("987654321")
                .status(DeliveryStatus.SUCCESS)
                .sentAt(yesterday)
                .build();
        message2.addDelivery(delivery2Telegram);

        entityManager.persist(message2);
        entityManager.flush();

        // Mensaje 3: Semana pasada solo Discord (SUCCESS)
        Message message3 = Message.builder()
                .user(testUser)
                .content("Last week's message - discord only")
                .createdAt(lastWeek)
                .build();

        MessageDelivery delivery3Discord = MessageDelivery.builder()
                .platformType(PlatformType.DISCORD)
                .destination("discord-channel-3")
                .status(DeliveryStatus.SUCCESS)
                .sentAt(lastWeek)
                .build();
        message3.addDelivery(delivery3Discord);

        entityManager.persist(message3);
        entityManager.flush();

        // Mensaje de otro usuario
        messageOtherUser = Message.builder()
                .user(anotherUser)
                .content("Another user's message")
                .createdAt(now)
                .build();

        MessageDelivery deliveryOtherUser = MessageDelivery.builder()
                .platformType(PlatformType.TELEGRAM)
                .destination("111222333")
                .status(DeliveryStatus.SUCCESS)
                .sentAt(now)
                .build();
        messageOtherUser.addDelivery(deliveryOtherUser);

        entityManager.persist(messageOtherUser);
        entityManager.flush();
    }

    @Test
    @DisplayName("Debe retornar todos los mensajes del usuario sin filtros")
    void findMessagesByFiltersNoFiltersReturnsAllUserMessages() {
        MessageFilterCriteria criteria = MessageFilterCriteria.builder()
                .user(testUser)
                .build();

        List<Message> result = messageDeliveryRepository.findMessagesByFilters(criteria);

        assertNotNull(result);
        assertEquals(3, result.size(), "Debe retornar los 3 mensajes del testUser");

        assertTrue(result.get(0).getCreatedAt().isAfter(result.get(1).getCreatedAt()));
        assertTrue(result.get(1).getCreatedAt().isAfter(result.get(2).getCreatedAt()));

        assertTrue(result.stream().allMatch(m -> m.getUser().equals(testUser)));
    }

    @Test
    @DisplayName("Debe filtrar mensajes por status SUCCESS")
    void findMessagesByFiltersStatusSuccessReturnsOnlySuccessMessages() {
        MessageFilterCriteria criteria = MessageFilterCriteria.builder()
                .user(testUser)
                .status(DeliveryStatus.SUCCESS)
                .build();

        List<Message> result = messageDeliveryRepository.findMessagesByFilters(criteria);

        assertNotNull(result);
        assertEquals(3, result.size(), "Los 3 mensajes tienen al menos un delivery SUCCESS");

        result.forEach(message -> {
            boolean hasSuccessDelivery = message.getDeliveries().stream()
                    .anyMatch(d -> d.getStatus() == DeliveryStatus.SUCCESS);
            assertTrue(hasSuccessDelivery, "Cada mensaje debe tener al menos un delivery SUCCESS");
        });
    }

    @Test
    @DisplayName("Debe filtrar mensajes por status FAILED")
    void findMessagesByFiltersStatusFailedReturnsOnlyFailedMessages() {
        MessageFilterCriteria criteria = MessageFilterCriteria.builder()
                .user(testUser)
                .status(DeliveryStatus.FAILED)
                .build();

        List<Message> result = messageDeliveryRepository.findMessagesByFilters(criteria);

        assertNotNull(result);
        assertEquals(1, result.size(), "Solo message2 tiene un delivery FAILED");
        assertEquals("Yesterday's message - mixed status", result.get(0).getContent());
    }

    @Test
    @DisplayName("Debe filtrar mensajes por platform DISCORD")
    void findMessagesByFiltersPlatformDiscordReturnsOnlyDiscordMessages() {
        MessageFilterCriteria criteria = MessageFilterCriteria.builder()
                .user(testUser)
                .platform(PlatformType.DISCORD)
                .build();

        List<Message> result = messageDeliveryRepository.findMessagesByFilters(criteria);

        assertNotNull(result);
        assertEquals(3, result.size(), "Los 3 mensajes tienen delivery a Discord");

        result.forEach(message -> {
            boolean hasDiscordDelivery = message.getDeliveries().stream()
                    .anyMatch(d -> d.getPlatformType() == PlatformType.DISCORD);
            assertTrue(hasDiscordDelivery, "Cada mensaje debe tener delivery a Discord");
        });
    }

    @Test
    @DisplayName("Debe filtrar mensajes por platform TELEGRAM")
    void findMessagesByFiltersPlatformTelegramReturnsOnlyTelegramMessages() {
        MessageFilterCriteria criteria = MessageFilterCriteria.builder()
                .user(testUser)
                .platform(PlatformType.TELEGRAM)
                .build();

        List<Message> result = messageDeliveryRepository.findMessagesByFilters(criteria);

        assertNotNull(result);
        assertEquals(2, result.size(), "Solo message1 y message2 tienen delivery a Telegram");

        result.forEach(message -> {
            boolean hasTelegramDelivery = message.getDeliveries().stream()
                    .anyMatch(d -> d.getPlatformType() == PlatformType.TELEGRAM);
            assertTrue(hasTelegramDelivery, "Cada mensaje debe tener delivery a Telegram");
        });
    }

    @Test
    @DisplayName("Debe filtrar mensajes por rango de fechas")
    void findMessagesByFiltersDateRangeReturnsMessagesInRange() {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(2);
        LocalDateTime toDate = LocalDateTime.now().plusHours(1);

        MessageFilterCriteria criteria = MessageFilterCriteria.builder()
                .user(testUser)
                .from(fromDate)
                .to(toDate)
                .build();

        List<Message> result = messageDeliveryRepository.findMessagesByFilters(criteria);

        assertNotNull(result);
        assertEquals(2, result.size(), "Solo message1 (hoy) y message2 (ayer) están en el rango");

        result.forEach(message -> {
            assertTrue(message.getCreatedAt().isAfter(fromDate) || message.getCreatedAt().isEqual(fromDate));
            assertTrue(message.getCreatedAt().isBefore(toDate) || message.getCreatedAt().isEqual(toDate));
        });
    }

    @Test
    @DisplayName("Debe filtrar con múltiples criterios combinados")
    void findMessagesByFiltersMultipleCriteriaCombined() {
        MessageFilterCriteria criteria = MessageFilterCriteria.builder()
                .user(testUser)
                .platform(PlatformType.TELEGRAM)
                .status(DeliveryStatus.SUCCESS)
                .build();

        List<Message> result = messageDeliveryRepository.findMessagesByFilters(criteria);

        assertNotNull(result);
        assertEquals(2, result.size(), "message1 y message2 tienen Telegram SUCCESS");

        result.forEach(message -> {
            boolean hasTelegramSuccess = message.getDeliveries().stream()
                    .anyMatch(d -> d.getPlatformType() == PlatformType.TELEGRAM
                            && d.getStatus() == DeliveryStatus.SUCCESS);
            assertTrue(hasTelegramSuccess);
        });
    }

    @Test
    @DisplayName("Debe filtrar con platform, status y fechas combinados")
    void findMessagesByFiltersAllCriteriasCombined() {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(2);
        LocalDateTime toDate = LocalDateTime.now().plusHours(1);

        MessageFilterCriteria criteria = MessageFilterCriteria.builder()
                .user(testUser)
                .platform(PlatformType.DISCORD)
                .status(DeliveryStatus.SUCCESS)
                .from(fromDate)
                .to(toDate)
                .build();

        List<Message> result = messageDeliveryRepository.findMessagesByFilters(criteria);

        assertNotNull(result);
        assertEquals(1, result.size(), "Solo message1 cumple todos los criterios");
        assertEquals("Today's message - both platforms success", result.get(0).getContent());
    }

    @Test
    @DisplayName("Debe retornar lista vacía cuando no hay coincidencias")
    void findMessagesByFiltersNoMatchesReturnsEmptyList() {
        MessageFilterCriteria criteria = MessageFilterCriteria.builder()
                .user(testUser)
                .status(DeliveryStatus.PENDING)
                .build();

        List<Message> result = messageDeliveryRepository.findMessagesByFilters(criteria);

        assertNotNull(result);
        assertTrue(result.isEmpty(), "No debe haber mensajes con status PENDING");
    }

    @Test
    @DisplayName("Debe retornar lista vacía para otro usuario")
    void findMessagesByFiltersDifferentUserReturnsEmpty() {
        MessageFilterCriteria criteria = MessageFilterCriteria.builder()
                .user(anotherUser)
                .build();

        List<Message> result = messageDeliveryRepository.findMessagesByFilters(criteria);

        assertNotNull(result);
        assertEquals(1, result.size(), "Debe retornar solo el mensaje de anotherUser");
        assertEquals(messageOtherUser.getId(), result.get(0).getId());
    }

    @Test
    @DisplayName("Debe retornar mensajes distintos (DISTINCT) cuando hay múltiples deliveries")
    void findMessagesByFiltersReturnsDistinctMessages() {
        MessageFilterCriteria criteria = MessageFilterCriteria.builder()
                .user(testUser)
                .status(DeliveryStatus.SUCCESS)
                .build();

        List<Message> result = messageDeliveryRepository.findMessagesByFilters(criteria);

        assertNotNull(result);

        long uniqueMessageIds = result.stream()
                .map(Message::getId)
                .distinct()
                .count();

        assertEquals(result.size(), uniqueMessageIds,
                "No debe haber mensajes duplicados en el resultado");
    }

    @Test
    @DisplayName("Debe retornar mensajes ordenados por fecha descendente")
    void findMessagesByFiltersReturnsMessagesInDescendingOrder() {
        MessageFilterCriteria criteria = MessageFilterCriteria.builder()
                .user(testUser)
                .build();

        List<Message> result = messageDeliveryRepository.findMessagesByFilters(criteria);

        assertNotNull(result);
        assertTrue(result.size() >= 2, "Debe haber al menos 2 mensajes para verificar orden");

        for (int i = 0; i < result.size() - 1; i++) {
            LocalDateTime current = result.get(i).getCreatedAt();
            LocalDateTime next = result.get(i + 1).getCreatedAt();

            assertTrue(current.isAfter(next) || current.isEqual(next),
                    "Los mensajes deben estar ordenados por fecha descendente");
        }
    }

    @Test
    @DisplayName("Debe manejar correctamente criterio con solo fecha FROM")
    void findMessagesByFiltersOnlyFromDate() {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(2);

        MessageFilterCriteria criteria = MessageFilterCriteria.builder()
                .user(testUser)
                .from(fromDate)
                .build();

        List<Message> result = messageDeliveryRepository.findMessagesByFilters(criteria);

        assertNotNull(result);
        assertEquals(2, result.size(), "Debe retornar mensajes desde hace 2 días");

        result.forEach(message -> {
            assertTrue(message.getCreatedAt().isAfter(fromDate)
                    || message.getCreatedAt().isEqual(fromDate));
        });
    }

    @Test
    @DisplayName("Debe manejar correctamente criterio con solo fecha TO")
    void findMessagesByFiltersOnlyToDate() {
        LocalDateTime toDate = LocalDateTime.now().minusDays(2);

        MessageFilterCriteria criteria = MessageFilterCriteria.builder()
                .user(testUser)
                .to(toDate)
                .build();

        List<Message> result = messageDeliveryRepository.findMessagesByFilters(criteria);

        assertNotNull(result);
        assertEquals(1, result.size(), "Solo message3 (semana pasada) es anterior a hace 2 días");

        result.forEach(message -> {
            assertTrue(message.getCreatedAt().isBefore(toDate)
                    || message.getCreatedAt().isEqual(toDate));
        });
    }
}