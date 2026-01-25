package com.notificationhub.security.service;

import com.notificationhub.entity.User;
import com.notificationhub.enums.Role;
import com.notificationhub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService Unit Tests")
public class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private User testUser;
    private User adminUser;
    private static final String TEST_USERNAME = "testuser";
    private static final String ADMIN_USERNAME = "adminuser";
    private static final String PASSWORD_HASH = "$2a$10$hashedPassword";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername(TEST_USERNAME);
        testUser.setPasswordHash(PASSWORD_HASH);
        testUser.setRole(Role.USER);

        adminUser = new User();
        adminUser.setId(2L);
        adminUser.setUsername(ADMIN_USERNAME);
        adminUser.setPasswordHash(PASSWORD_HASH);
        adminUser.setRole(Role.ADMIN);
    }

    // ========== LOAD USER BY USERNAME ==========

    @Test
    @DisplayName("Should load user by username successfully")
    void shouldLoadUserByUsernameSuccessfully() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername(TEST_USERNAME);

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(TEST_USERNAME);
        assertThat(userDetails.getPassword()).isEqualTo(PASSWORD_HASH);
        verify(userRepository).findByUsername(TEST_USERNAME);
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        String nonExistentUsername = "nonexistent";
        when(userRepository.findByUsername(nonExistentUsername)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(nonExistentUsername))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found with username: " + nonExistentUsername);

        verify(userRepository).findByUsername(nonExistentUsername);
    }

    // ========== AUTHORITIES ==========

    @Test
    @DisplayName("Should map USER role to ROLE_USER authority")
    void shouldMapUserRoleToAuthority() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername(TEST_USERNAME);

        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        assertThat(authorities).hasSize(1);
        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("Should map ADMIN role to ROLE_ADMIN authority")
    void shouldMapAdminRoleToAuthority() {
        when(userRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(adminUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername(ADMIN_USERNAME);

        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        assertThat(authorities).hasSize(1);
        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    // ========== USER DETAILS PROPERTIES ==========

    @Test
    @DisplayName("Should set username correctly in UserDetails")
    void shouldSetUsernameCorrectly() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername(TEST_USERNAME);

        assertThat(userDetails.getUsername()).isEqualTo(TEST_USERNAME);
    }

    @Test
    @DisplayName("Should set password hash correctly in UserDetails")
    void shouldSetPasswordHashCorrectly() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername(TEST_USERNAME);

        assertThat(userDetails.getPassword()).isEqualTo(PASSWORD_HASH);
    }

    @Test
    @DisplayName("Should set account as not expired")
    void shouldSetAccountAsNotExpired() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername(TEST_USERNAME);

        assertThat(userDetails.isAccountNonExpired()).isTrue();
    }

    @Test
    @DisplayName("Should set account as not locked")
    void shouldSetAccountAsNotLocked() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername(TEST_USERNAME);

        assertThat(userDetails.isAccountNonLocked()).isTrue();
    }

    @Test
    @DisplayName("Should set credentials as not expired")
    void shouldSetCredentialsAsNotExpired() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername(TEST_USERNAME);

        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
    }

    @Test
    @DisplayName("Should set account as enabled")
    void shouldSetAccountAsEnabled() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername(TEST_USERNAME);

        assertThat(userDetails.isEnabled()).isTrue();
    }

    // ========== EDGE CASES ==========

    @Test
    @DisplayName("Should handle username with special characters")
    void shouldHandleUsernameWithSpecialCharacters() {
        String specialUsername = "user@example.com";
        testUser.setUsername(specialUsername);
        when(userRepository.findByUsername(specialUsername)).thenReturn(Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername(specialUsername);

        assertThat(userDetails.getUsername()).isEqualTo(specialUsername);
    }

    @Test
    @DisplayName("Should handle case-sensitive usernames")
    void shouldHandleCaseSensitiveUsernames() {
        String upperCaseUsername = "TESTUSER";
        when(userRepository.findByUsername(upperCaseUsername)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(upperCaseUsername))
                .isInstanceOf(UsernameNotFoundException.class);

        verify(userRepository).findByUsername(upperCaseUsername);
        verify(userRepository, never()).findByUsername(TEST_USERNAME.toLowerCase());
    }

    // ========== INTEGRATION SCENARIOS ==========

    @Test
    @DisplayName("Should correctly build UserDetails for authentication flow")
    void shouldBuildUserDetailsForAuthenticationFlow() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername(TEST_USERNAME);

        // Verify all properties needed for Spring Security authentication
        assertThat(userDetails.getUsername()).isEqualTo(TEST_USERNAME);
        assertThat(userDetails.getPassword()).isEqualTo(PASSWORD_HASH);
        assertThat(userDetails.getAuthorities()).isNotEmpty();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        assertThat(userDetails.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should call repository exactly once per load")
    void shouldCallRepositoryOncePerLoad() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));

        userDetailsService.loadUserByUsername(TEST_USERNAME);

        verify(userRepository, times(1)).findByUsername(TEST_USERNAME);
    }
}
