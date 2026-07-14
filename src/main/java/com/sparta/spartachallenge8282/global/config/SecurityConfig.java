package com.sparta.spartachallenge8282.global.config;

import com.sparta.spartachallenge8282.global.security.AuthEntryPoint;
import com.sparta.spartachallenge8282.global.security.JwtAuthFilter;
import com.sparta.spartachallenge8282.global.security.JwtProvider;
import com.sparta.spartachallenge8282.user.repository.UserRepository;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

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
            // 배포 헬스체크 (인증과 무관하게 항상 200 응답)
            "/actuator/health",
            "/api/users/signup",
            "/api/users/login",
            // Swagger (추후 추가 시)
            "/api/v1/auth/signup",
            "/api/v1/auth/login",
            "/api/v1/auth/reissue", // 토큰 재발급은 필터를 거치지 않거나 통과시킨다
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(authEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/stores/**", "/api/v1/products/**",
                                "/api/v1/regions/**", "/api/v1/categories/**",
                                "/api/v1/menus/**", "/api/v1/option-groups/**",
                                "/api/v1/options/**").permitAll()
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

    /**
     * 개발용 CORS 설정.
     * 테스트 프론트엔드(front/)가 브라우저에서 API 를 호출할 수 있도록 허용한다.
     * Bearer 토큰 방식이므로 쿠키 인증(allowCredentials)은 사용하지 않으며,
     * 커스텀 헤더(Idempotency-Key 등)를 위해 모든 헤더를 허용한다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 로컬 개발 편의상 모든 오리진 패턴 허용 (file://, localhost, 127.0.0.1 등)
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Idempotency-Key"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
