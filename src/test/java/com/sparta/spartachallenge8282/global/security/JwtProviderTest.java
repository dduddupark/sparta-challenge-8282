package com.sparta.spartachallenge8282.global.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(
                "test-secret-key-must-be-at-least-32-characters-long",
                900_000L,
                604_800_000L
        );
    }

    @Test
    @DisplayName("Access Token은 Access Token 검증만 통과한다")
    void accessToken_isRejectedAsRefreshToken() {
        String accessToken = jwtProvider.createAccessToken(
                1L, "test@sparta.com", "ROLE_CUSTOMER", 0L);

        assertThat(jwtProvider.validateAccessToken(accessToken)).isTrue();
        assertThat(jwtProvider.validateRefreshToken(accessToken)).isNull();
    }

    @Test
    @DisplayName("Refresh Token은 Access Token으로 사용할 수 없다")
    void refreshToken_isRejectedAsAccessToken() {
        String refreshToken = jwtProvider.createRefreshToken("test@sparta.com");

        assertThat(jwtProvider.validateRefreshToken(refreshToken)).isEqualTo("test@sparta.com");
        assertThat(jwtProvider.validateAccessToken(refreshToken)).isFalse();
    }
}
