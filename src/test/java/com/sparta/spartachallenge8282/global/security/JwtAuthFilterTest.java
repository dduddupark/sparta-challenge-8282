package com.sparta.spartachallenge8282.global.security;

import com.sparta.spartachallenge8282.user.domain.User;
import com.sparta.spartachallenge8282.user.domain.UserRepository;
import com.sparta.spartachallenge8282.user.domain.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class JwtAuthFilterTest {

    private JwtProvider jwtProvider;
    private UserRepository userRepository;
    private JwtAuthFilter jwtAuthFilter;
    private User user;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(
                "test-secret-key-must-be-at-least-32-characters-long",
                900_000L,
                604_800_000L
        );
        userRepository = mock(UserRepository.class);
        jwtAuthFilter = new JwtAuthFilter(jwtProvider, userRepository);
        user = User.builder()
                .email("test@sparta.com")
                .password("encoded")
                .nickname("테스터")
                .address("서울")
                .role(UserRole.CUSTOMER)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        given(userRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(user));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("DB와 토큰 버전이 같으면 인증된다")
    void matchingTokenVersion_authenticates() throws Exception {
        String token = jwtProvider.createAccessToken(
                1L, user.getEmail(), user.getRole().getAuthority(), user.getTokenVersion());

        executeFilter(token);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    @DisplayName("로그아웃 이전에 발급된 Access Token은 인증되지 않는다")
    void invalidatedTokenVersion_doesNotAuthenticate() throws Exception {
        String token = jwtProvider.createAccessToken(
                1L, user.getEmail(), user.getRole().getAuthority(), user.getTokenVersion());
        user.invalidateTokens();

        executeFilter(token);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private void executeFilter(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        jwtAuthFilter.doFilter(
                request,
                new MockHttpServletResponse(),
                new MockFilterChain()
        );
    }
}
