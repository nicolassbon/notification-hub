package com.notificationhub.repository;

import com.notificationhub.entity.DailyMessageCount;
import com.notificationhub.entity.User;
import com.notificationhub.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("DailyMessageCountRepository Unit Tests")
public class DailyMessageCountRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DailyMessageCountRepository dailyMessageCountRepository;

    private User testUser;
    private User anotherUser;
    private DailyMessageCount todayCount;

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

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        todayCount = DailyMessageCount.builder()
                .user(testUser)
                .date(today)
                .count(5)
                .build();

        DailyMessageCount yesterdayCount = DailyMessageCount.builder()
                .user(testUser)
                .date(yesterday)
                .count(10)
                .build();

        DailyMessageCount otherUserCount = DailyMessageCount.builder()
                .user(anotherUser)
                .date(today)
                .count(3)
                .build();

        entityManager.persist(todayCount);
        entityManager.persist(yesterdayCount);
        entityManager.persist(otherUserCount);
        entityManager.flush();
    }

    @Test
    @DisplayName("Should find daily count by user and date")
    void findByUserAndDateWhenCountExistsReturnsCount() {
        LocalDate today = LocalDate.now();

        Optional<DailyMessageCount> result = dailyMessageCountRepository.findByUserAndDate(testUser, today);

        assertTrue(result.isPresent());
        assertEquals(todayCount.getId(), result.get().getId());
        assertEquals(5, result.get().getCount());
        assertEquals(testUser, result.get().getUser());
        assertEquals(today, result.get().getDate());
    }

    @Test
    @DisplayName("Should return empty when no count exists for user and date")
    void findByUserAndDateWhenNoCountExistsReturnsEmpty() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        Optional<DailyMessageCount> result = dailyMessageCountRepository.findByUserAndDate(testUser, tomorrow);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should find different counts for same user on different dates")
    void findByUserAndDateDifferentDatesReturnDifferentCounts() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        Optional<DailyMessageCount> todayResult = dailyMessageCountRepository.findByUserAndDate(testUser, today);
        Optional<DailyMessageCount> yesterdayResult = dailyMessageCountRepository.findByUserAndDate(testUser, yesterday);

        assertTrue(todayResult.isPresent());
        assertTrue(yesterdayResult.isPresent());
        assertEquals(5, todayResult.get().getCount());
        assertEquals(10, yesterdayResult.get().getCount());
    }

    @Test
    @DisplayName("Should not return counts for other users")
    void findByUserAndDateOnlyReturnsForRequestedUser() {
        LocalDate today = LocalDate.now();

        Optional<DailyMessageCount> testUserResult = dailyMessageCountRepository.findByUserAndDate(testUser, today);
        Optional<DailyMessageCount> anotherUserResult = dailyMessageCountRepository.findByUserAndDate(anotherUser, today);

        assertTrue(testUserResult.isPresent());
        assertTrue(anotherUserResult.isPresent());
        assertEquals(testUser, testUserResult.get().getUser());
        assertEquals(anotherUser, anotherUserResult.get().getUser());
        assertEquals(5, testUserResult.get().getCount());
        assertEquals(3, anotherUserResult.get().getCount());
    }

    @Test
    @DisplayName("Should handle user with no counts")
    void findByUserAndDateUserWithNoCountsReturnsEmpty() {
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

        LocalDate today = LocalDate.now();

        Optional<DailyMessageCount> result = dailyMessageCountRepository.findByUserAndDate(newUser, today);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should save and retrieve new daily count")
    void saveNewDailyCountPersistsCorrectly() {
        User newUser = User.builder()
                .username("saveuser")
                .passwordHash("$2a$10$hashedpassword4")
                .role(Role.USER)
                .dailyMessageLimit(100)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        entityManager.persist(newUser);
        entityManager.flush();

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        DailyMessageCount newCount = DailyMessageCount.builder()
                .user(newUser)
                .date(tomorrow)
                .count(7)
                .build();

        DailyMessageCount savedCount = dailyMessageCountRepository.save(newCount);
        entityManager.flush();
        entityManager.clear();

        Optional<DailyMessageCount> retrievedCount = dailyMessageCountRepository.findByUserAndDate(newUser, tomorrow);
        assertTrue(retrievedCount.isPresent());
        assertEquals(7, retrievedCount.get().getCount());
        assertEquals(newUser, retrievedCount.get().getUser());
        assertEquals(tomorrow, retrievedCount.get().getDate());
    }

    @Test
    @DisplayName("Should update existing daily count")
    void saveExistingDailyCountUpdatesCorrectly() {
        LocalDate today = LocalDate.now();
        todayCount.setCount(15);

        DailyMessageCount updatedCount = dailyMessageCountRepository.save(todayCount);
        entityManager.flush();
        entityManager.clear();

        Optional<DailyMessageCount> retrievedCount = dailyMessageCountRepository.findByUserAndDate(testUser, today);
        assertTrue(retrievedCount.isPresent());
        assertEquals(15, retrievedCount.get().getCount());
    }
}
