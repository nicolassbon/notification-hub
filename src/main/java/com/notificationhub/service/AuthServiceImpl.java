package com.notificationhub.service;

import com.notificationhub.dto.request.LoginRequest;
import com.notificationhub.dto.request.RegisterRequest;
import com.notificationhub.dto.response.AuthResponse;
import com.notificationhub.dto.response.RegisterResponse;
import com.notificationhub.entity.User;
import com.notificationhub.enums.Role;
import com.notificationhub.exception.InvalidCredentialsException;
import com.notificationhub.repository.UserRepository;
import com.notificationhub.util.JwtUtils;
import com.notificationhub.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
public class AuthServiceImpl implements IAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final SecurityUtils securityUtils;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtils jwtUtils,
                           AuthenticationManager authenticationManager,
                           SecurityUtils securityUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.authenticationManager = authenticationManager;
        this.securityUtils = securityUtils;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        log.info("Attempting to register user: {}", request.getUsername());

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .dailyMessageLimit(100)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getUsername());

        return RegisterResponse.builder()
                .message("User registered successfully")
                .username(savedUser.getUsername())
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Autentica un usuario existente
     */
    public AuthResponse login(LoginRequest request) {
        log.info("Attempting login for user: {}", request.getUsername());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

            String token = jwtUtils.generateToken(userDetails);

            log.info("Login successful for user: {}", user.getUsername());

            return AuthResponse.builder()
                    .token(token)
                    .expiresIn(jwtUtils.getExpirationTime())
                    .username(user.getUsername())
                    .role(user.getRole().name())
                    .build();

        } catch (Exception e) {
            log.warn("Login failed for user: {} - {}", request.getUsername(), e.getMessage());
            throw new InvalidCredentialsException("Invalid username or password");
        }
    }


    /**
     * Obtiene información del usuario actual (USANDO SecurityUtils)
     */
    public User getCurrentUser() {
        User currentUser = securityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new InvalidCredentialsException("Invalid credentials");
        }
        return currentUser;
    }

    /**
     * Verifica si el usuario actual puede realizar acciones de admin
     */
    public boolean isCurrentUserAdmin() {
        return securityUtils.isAdmin();
    }
}
