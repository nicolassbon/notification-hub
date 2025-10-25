package com.notificationhub.repository;

import com.notificationhub.entity.User;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository Unit Tests")
public class UserRepositoryTest {
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User activeUser;

    @BeforeEach
    void setUp() {
        entityManager.clear();

        activeUser = User.builder()
                .username("testuser")
                .passwordHash("$2a$10$hashedpassword")
                .role(Role.USER)
                .dailyMessageLimit(100)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User adminUser = User.builder()
                .username("admin")
                .passwordHash("$2a$10$hashedadminpassword")
                .role(Role.ADMIN)
                .dailyMessageLimit(1000)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        entityManager.persist(activeUser);
        entityManager.persist(adminUser);
        entityManager.flush();
    }

    @Test
    @DisplayName("Should find user by username when user exists and is active")
    void findByUsernameWhenUserExistsAndActiveReturnsUser() {
        Optional<User> foundUser = userRepository.findByUsername("testuser");

        assertTrue(foundUser.isPresent());
        assertEquals("testuser", foundUser.get().getUsername());
        assertEquals(Role.USER, foundUser.get().getRole());
    }

    @Test
    @DisplayName("Should not find user by username when user is deleted")
    void findByUsernameWhenUserIsDeletedReturnsEmpty() {
        Optional<User> foundUser = userRepository.findByUsername("deleteduser");

        assertFalse(foundUser.isPresent());
    }

    @Test
    @DisplayName("Should not find user by username when user does not exist")
    void findByUsernameWhenUserDoesNotExistReturnsEmpty() {
        Optional<User> foundUser = userRepository.findByUsername("nonexistent");

        assertFalse(foundUser.isPresent());
    }

    @Test
    @DisplayName("Should return true when username exists and user is active")
    void existsByUsernameWhenUserExistsAndActiveReturnsTrue() {
        boolean exists = userRepository.existsByUsername("testuser");

        assertTrue(exists);
    }

    @Test
    @DisplayName("Should return false when username exists but user is deleted")
    void existsByUsernameWhenUserIsDeletedReturnsFalse() {
        boolean exists = userRepository.existsByUsername("deleteduser");

        assertFalse(exists);
    }

    @Test
    @DisplayName("Should return false when username does not exist")
    void existsByUsernameWhenUserDoesNotExistReturnsFalse() {
        boolean exists = userRepository.existsByUsername("nonexistent");

        assertFalse(exists);
    }

    @Test
    @DisplayName("Should find all users")
    void findAllActiveReturnsOnlyActiveUsers() {
        List<User> activeUsers = userRepository.findAll();

        assertNotNull(activeUsers);
        assertEquals(2, activeUsers.size());
        assertTrue(activeUsers.stream().anyMatch(user -> user.getUsername().equals("testuser")));
        assertTrue(activeUsers.stream().anyMatch(user -> user.getUsername().equals("admin")));
        assertFalse(activeUsers.stream().anyMatch(user -> user.getUsername().equals("deleteduser")));
    }

    @Test
    @DisplayName("Should find user by ID when user exists")
    void findByIdAndActiveWhenUserExistsAndActiveReturnsUser() {
        Optional<User> foundUser = userRepository.findById(activeUser.getId());

        assertTrue(foundUser.isPresent());
        assertEquals(activeUser.getId(), foundUser.get().getId());
        assertEquals("testuser", foundUser.get().getUsername());
    }

    @Test
    @DisplayName("Should not find user by ID when user does not exist")
    void findByIdAndActiveWhenUserDoesNotExistReturnsEmpty() {
        Optional<User> foundUser = userRepository.findById(999L);

        assertFalse(foundUser.isPresent());
    }

    @Test
    @DisplayName("Should save new user successfully")
    void saveNewUserSavesSuccessfully() {
        User newUser = User.builder()
                .username("newuser")
                .passwordHash("$2a$10$newhashedpassword")
                .role(Role.USER)
                .dailyMessageLimit(100)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(newUser);
        entityManager.flush();
        entityManager.clear();

        assertNotNull(savedUser.getId());
        Optional<User> foundUser = userRepository.findByUsername("newuser");
        assertTrue(foundUser.isPresent());
        assertEquals("newuser", foundUser.get().getUsername());
    }

    @Test
    @DisplayName("Should update existing user successfully")
    void saveExistingUserUpdatesSuccessfully() {
        activeUser.setDailyMessageLimit(200);
        activeUser.setUpdatedAt(LocalDateTime.now());

        User updatedUser = userRepository.save(activeUser);
        entityManager.flush();
        entityManager.clear();

        Optional<User> foundUser = userRepository.findByUsername("testuser");
        assertTrue(foundUser.isPresent());
        assertEquals(200, foundUser.get().getDailyMessageLimit());
    }

    @Test
    @DisplayName("Should find users with different roles correctly")
    void findByUsernameWithDifferentRolesReturnsCorrectUser() {
        Optional<User> userResult = userRepository.findByUsername("testuser");
        Optional<User> adminResult = userRepository.findByUsername("admin");

        assertTrue(userResult.isPresent());
        assertEquals(Role.USER, userResult.get().getRole());

        assertTrue(adminResult.isPresent());
        assertEquals(Role.ADMIN, adminResult.get().getRole());
    }

    @Test
    @DisplayName("Should handle case sensitivity in usernames")
    void findByUsernameWithDifferentCaseReturnsEmpty() {
        Optional<User> foundUser = userRepository.findByUsername("TESTUSER"); // uppercase

        assertFalse(foundUser.isPresent());
    }

    @Test
    @DisplayName("Should return correct daily message limit for active users")
    void findByUsernameActiveUserReturnsCorrectDailyLimit() {
        Optional<User> foundUser = userRepository.findByUsername("testuser");

        assertTrue(foundUser.isPresent());
        assertEquals(100, foundUser.get().getDailyMessageLimit());
    }
}
