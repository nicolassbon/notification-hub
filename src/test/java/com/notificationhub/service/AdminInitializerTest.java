package com.notificationhub.service;

import com.notificationhub.entity.User;
import com.notificationhub.enums.Role;
import com.notificationhub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminInitializer Unit Tests")
public class AdminInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationArguments applicationArguments;

    private AdminInitializer adminInitializer;

    private final String ADMIN_USERNAME = "admin";
    private final String ADMIN_PASSWORD = "securePassword123";

    @BeforeEach
    void setUp() {
        adminInitializer = new AdminInitializer(
                userRepository,
                passwordEncoder,
                ADMIN_USERNAME,
                ADMIN_PASSWORD
        );
    }

    @Test
    @DisplayName("Should create admin user if it does not exist")
    void runCreatesAdminWhenNotExists() throws Exception {
        when(userRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(ADMIN_PASSWORD)).thenReturn("encodedhash");

        adminInitializer.run(applicationArguments);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals(ADMIN_USERNAME, savedUser.getUsername());
        assertEquals("encodedhash", savedUser.getPasswordHash());
        assertEquals(Role.ADMIN, savedUser.getRole());
        assertEquals(1000, savedUser.getDailyMessageLimit());
    }

    @Test
    @DisplayName("Should NOT create admin user if it already exists")
    void runDoesNothingWhenAdminExists() throws Exception {
        User existingAdmin = User.builder().username(ADMIN_USERNAME).build();
        when(userRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(existingAdmin));

        adminInitializer.run(applicationArguments);

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Should handle exceptions gracefully")
    void runHandlesExceptionGracefully() throws Exception {
        when(userRepository.findByUsername(ADMIN_USERNAME)).thenThrow(new RuntimeException("DB Connection failed"));

        adminInitializer.run(applicationArguments);

        verify(userRepository, never()).save(any(User.class));
    }
}
