package com.notificationhub.security;

import com.notificationhub.entity.User;
import com.notificationhub.enums.Role;
import com.notificationhub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService Unit Tests")
public class CustomUserDetailsServiceTest {
    @Mock
    private UserRepository userRepository;

    private CustomUserDetailsService userDetailsService;

    private User activeUser;
    private User adminUser;
    private User deletedUser;

    @BeforeEach
    void setUp() {
        userDetailsService = new CustomUserDetailsService(userRepository);

        activeUser = User.builder()
                .id(1L)
                .username("testuser")
                .passwordHash("$2a$10$hashedpassword123")
                .role(Role.USER)
                .dailyMessageLimit(100)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deletedAt(null)
                .build();

        adminUser = User.builder()
                .id(2L)
                .username("admin")
                .passwordHash("$2a$10$hashedadminpassword")
                .role(Role.ADMIN)
                .dailyMessageLimit(1000)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deletedAt(null)
                .build();

        deletedUser = User.builder()
                .id(3L)
                .username("deleteduser")
                .passwordHash("$2a$10$hasheddeletedpassword")
                .role(Role.USER)
                .dailyMessageLimit(50)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deletedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should load user by username when user exists and is active")
    void loadUserByUsernameWhenUserExistsAndActiveReturnsUserDetails() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        assertEquals("$2a$10$hashedpassword123", userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isCredentialsNonExpired());

        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")));

        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should load admin user with correct authorities")
    void loadUserByUsernameWhenAdminUserReturnsAdminUserDetails() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("admin");

        assertNotNull(userDetails);
        assertEquals("admin", userDetails.getUsername());
        assertEquals("$2a$10$hashedadminpassword", userDetails.getPassword());
        assertTrue(userDetails.isEnabled());

        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")));

        verify(userRepository).findByUsername("admin");
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when user does not exist")
    void loadUserByUsernameWhenUserDoesNotExistThrowsException() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("nonexistent")
        );

        assertEquals("User not found with username: nonexistent", exception.getMessage());
        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when user is deleted")
    void loadUserByUsernameWhenUserIsDeletedThrowsException() {
        when(userRepository.findByUsername("deleteduser")).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("deleteduser")
        );

        assertEquals("User not found with username: deleteduser", exception.getMessage());
        verify(userRepository).findByUsername("deleteduser");
    }

    @Test
    @DisplayName("Should assign correct authorities based on user role")
    void getAuthoritiesWithDifferentRolesReturnsCorrectAuthorities() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        // Test USER role
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")));
        assertFalse(userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")));

        // Test ADMIN role
        UserDetails adminDetails = userDetailsService.loadUserByUsername("admin");
        assertTrue(adminDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("Should handle case sensitivity in usernames")
    void loadUserByUsernameWithDifferentCaseThrowsException() {
        when(userRepository.findByUsername("TESTUSER")).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("TESTUSER")
        );

        assertEquals("User not found with username: TESTUSER", exception.getMessage());
        verify(userRepository).findByUsername("TESTUSER");
    }

    @Test
    @DisplayName("Should handle null username gracefully")
    void loadUserByUsernameWithNullUsernameThrowsException() {
        assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(null)
        );
    }

    @Test
    @DisplayName("Should handle empty username gracefully")
    void loadUserByUsernameWithEmptyUsernameThrowsException() {
        when(userRepository.findByUsername("")).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("")
        );

        assertEquals("User not found with username: ", exception.getMessage());
        verify(userRepository).findByUsername("");
    }

    @Test
    @DisplayName("Should maintain user account status correctly")
    void buildUserFromUserWithActiveUserReturnsFullyEnabledUser() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        assertTrue(userDetails.isEnabled(), "User should be enabled");
        assertTrue(userDetails.isAccountNonLocked(), "Account should not be locked");
        assertTrue(userDetails.isAccountNonExpired(), "Account should not be expired");
        assertTrue(userDetails.isCredentialsNonExpired(), "Credentials should not be expired");
    }

    @Test
    @DisplayName("Should call repository only once per invocation")
    void loadUserByUsernameVerifiesSingleRepositoryCall() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));

        userDetailsService.loadUserByUsername("testuser");

        verify(userRepository, times(1)).findByUsername("testuser");
        verifyNoMoreInteractions(userRepository);
    }
}
