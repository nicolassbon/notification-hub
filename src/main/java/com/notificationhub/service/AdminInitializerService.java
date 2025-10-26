package com.notificationhub.service;

import com.notificationhub.entity.User;
import com.notificationhub.enums.Role;
import com.notificationhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AdminInitializerService implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        initializeAdminUser();
    }

    private void initializeAdminUser() {
        try {
            if (userRepository.findByUsername("admin").isEmpty()) {
                User admin = User.builder()
                        .username("admin")
                        .passwordHash(passwordEncoder.encode("admin123"))
                        .role(Role.ADMIN)
                        .dailyMessageLimit(1000)
                        .build();

                userRepository.save(admin);
                log.info("Usuario admin creado automáticamente. Username: admin, Password: admin123");
            }
        } catch (Exception e) {
            log.error("Error creando usuario admin: {}", e.getMessage());
        }
    }
}