package com.sparta.spartachallenge8282.global.config;

import com.sparta.spartachallenge8282.global.security.AuthEntryPoint;
import com.sparta.spartachallenge8282.global.security.JwtAuthFilter;
import com.sparta.spartachallenge8282.global.security.JwtProvider;
import com.sparta.spartachallenge8282.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정.
 * - Stateless (JWT 방식이므로 세션 미사용)
 * - CSRF 비활성화 (REST API)
 * - JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 등록
 */
@Configuration
//Spring Security 동작
@EnableWebSecurity
//메서드 단위 권한 체크 활성화
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final AuthEntryPoint authEntryPoint;
    private final UserRepository userRepository; // 실시간 회원 검증용 리포지토리 추가

    /**
     * 인증 없이 접근 가능한 경로 화이트리스트 (v1 API 설계 기준 반영)
     */
    private static final String[] PUBLIC_URLS = {
            "/api/v1/auth/signup",
            "/api/v1/auth/login",
            "/api/v1/auth/reissue", // 토큰 재발급은 필터를 거치지 않거나 통과시킨다
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            // User 로그인 기능 구현 전 주문 생성 테스트를 위한 임시 허용
            "/api/v1/orders",
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(authEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/stores/**", "/api/v1/products/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        new JwtAuthFilter(jwtProvider, userRepository), // 리포지토리 의존성 전달
                        UsernamePasswordAuthenticationFilter.class
                )
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
