package com.notificationhub.service;

import com.notificationhub.entity.User;
import com.notificationhub.enums.Role;
import com.notificationhub.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public AdminInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${admin.username}") String adminUsername,
            @Value("${admin.password}") String adminPassword
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        initializeAdminUser();
    }

    private void initializeAdminUser() {
        try {
            if (userRepository.findByUsername(adminUsername).isEmpty()) {
                User admin = User.builder()
                        .username(adminUsername)
                        .passwordHash(passwordEncoder.encode(adminPassword))
                        .role(Role.ADMIN)
                        .dailyMessageLimit(1000)
                        .build();

                userRepository.save(admin);
                log.info("Usuario admin creado exitosamente");
            }
        } catch (Exception e) {
            log.error("Error creando usuario admin: {}", e.getMessage());
        }
    }
}