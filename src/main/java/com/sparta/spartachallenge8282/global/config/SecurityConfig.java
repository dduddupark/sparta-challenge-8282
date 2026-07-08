package com.sparta.spartachallenge8282.global.config;

import com.sparta.spartachallenge8282.global.security.AuthEntryPoint;
import com.sparta.spartachallenge8282.global.security.JwtAuthFilter;
import com.sparta.spartachallenge8282.global.security.JwtProvider;
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
@EnableWebSecurity
@EnableMethodSecurity           // @PreAuthorize, @Secured 등 메서드 레벨 보안 활성화
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider     jwtProvider;
    private final AuthEntryPoint  authEntryPoint;

    /** 인증 없이 접근 가능한 경로 화이트리스트 */
    private static final String[] PUBLIC_URLS = {
            "/api/users/signup",
            "/api/users/login",
            // User 로그인 기능 구현 전 주문 생성 테스트를 위한 임시 허용
            "/api/v1/orders",
            // Swagger (추후 추가 시)
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
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
                        .requestMatchers(HttpMethod.GET, "/api/stores/**", "/api/products/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        new JwtAuthFilter(jwtProvider),
                        UsernamePasswordAuthenticationFilter.class
                )
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
