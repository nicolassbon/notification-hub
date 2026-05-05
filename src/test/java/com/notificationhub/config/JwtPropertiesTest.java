package com.notificationhub.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

    private static final String SECRET_WITH_EXACTLY_32_ASCII_BYTES = "12345678901234567890123456789012";
    private static final String SECRET_WITH_31_ASCII_BYTES = "1234567890123456789012345678901";
    private static final String SECRET_WITH_32_UTF8_BYTES_AND_8_CHARS = "😀😀😀😀😀😀😀😀";

    @Test
    void passes_when_secret_utf8_length_is_exactly_32_bytes() {
        JwtProperties jwtProperties = new JwtProperties(SECRET_WITH_EXACTLY_32_ASCII_BYTES, 3_600_000L);

        assertThatCode(jwtProperties::validateSecretAtStartup)
                .doesNotThrowAnyException();
    }

    @Test
    void throws_when_secret_utf8_length_is_less_than_32_bytes() {
        JwtProperties jwtProperties = new JwtProperties(SECRET_WITH_31_ASCII_BYTES, 3_600_000L);

        assertThatThrownBy(jwtProperties::validateSecretAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    @Test
    void throws_when_secret_is_null() {
        JwtProperties jwtProperties = new JwtProperties(null, 3_600_000L);

        assertThatThrownBy(jwtProperties::validateSecretAtStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    @Test
    void passes_when_secret_has_multibyte_chars_but_utf8_length_is_at_least_32_bytes() {
        JwtProperties jwtProperties = new JwtProperties(SECRET_WITH_32_UTF8_BYTES_AND_8_CHARS, 3_600_000L);

        assertThatCode(jwtProperties::validateSecretAtStartup)
                .doesNotThrowAnyException();
    }
}
