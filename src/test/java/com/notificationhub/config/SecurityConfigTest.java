package com.notificationhub.config;

import com.notificationhub.security.filter.JwtAuthFilter;
import com.notificationhub.security.handlers.JwtAccessDeniedHandler;
import com.notificationhub.security.handlers.JwtAuthenticationEntryPoint;
import com.notificationhub.security.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private JwtAuthFilter jwtAuthFilter;

    @Mock
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Mock
    private JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Mock
    private AuthenticationConfiguration authenticationConfiguration;

    @Mock
    private AuthenticationManager authenticationManager;

    @Test
    void returnsBcryptPasswordEncoder() {
        SecurityConfig securityConfig = newSecurityConfig();

        var passwordEncoder = securityConfig.passwordEncoder();

        assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    void createsDaoAuthenticationProvider() {
        SecurityConfig securityConfig = newSecurityConfig();

        var authenticationProvider = securityConfig.authenticationProvider();

        assertThat(authenticationProvider).isNotNull();
    }

    @Test
    void delegatesAuthenticationManagerToSpringConfiguration() throws Exception {
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);
        SecurityConfig securityConfig = newSecurityConfig();

        var result = securityConfig.authenticationManager(authenticationConfiguration);

        assertThat(result).isSameAs(authenticationManager);
        verify(authenticationConfiguration).getAuthenticationManager();
    }

    private SecurityConfig newSecurityConfig() {
        return new SecurityConfig(
                userDetailsService,
                jwtAuthFilter,
                jwtAuthenticationEntryPoint,
                jwtAccessDeniedHandler);
    }
}
