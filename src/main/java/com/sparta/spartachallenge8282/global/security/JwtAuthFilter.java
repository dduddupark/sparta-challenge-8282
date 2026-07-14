package com.sparta.spartachallenge8282.global.security;

import com.sparta.spartachallenge8282.user.domain.User;
import com.sparta.spartachallenge8282.user.domain.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 인증 필터.
 * Authorization 헤더에서 Bearer 액세스 토큰을 추출·검증한 후,
 * DB 실시간 조회를 통해 유저 존재 여부(탈퇴 상태 검사)와 최신 권한을 검증한 뒤 SecurityContext에 인증 정보를 설정한다.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = jwtProvider.resolveToken(bearerToken);

        if (token != null && jwtProvider.validateAccessToken(token)) {
            setAuthentication(token);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * DB에서 유저 최신 상태를 조회하여 인증을 관리한다.
     * <p>성능 향상을 위해 인덱스가 잡혀있는 PK(userId) 기반 조회를 수행하며,
     * JWT 2중 파싱 방지를 위해 헬퍼 메서드를 통해 Claims를 직접 조회한다.
     */
    private void setAuthentication(String token) {
        Claims claims = jwtProvider.parseClaims(token);
        Long userId = claims.get(JwtProvider.CLAIM_USER_ID, Long.class);
        Object tokenVersionClaim = claims.get(JwtProvider.CLAIM_TOKEN_VERSION);

        // 1. PK 기반 고속 DB 조회로 유저 유효성 및 최신 권한 로드
        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElse(null);

        if (user == null) {
            log.warn("[JWT Auth] 인증 실패 - 회원 정보를 찾을 수 없거나 탈퇴한 회원입니다. userId={}", userId);
            return;
        }

        if (!(tokenVersionClaim instanceof Number tokenVersion)
                || tokenVersion.longValue() != user.getTokenVersion()) {
            log.warn("[JWT Auth] 인증 실패 - 무효화된 액세스 토큰입니다. userId={}", userId);
            return;
        }

        // 2. DB의 최신 권한 추출 (토큰 내부 권한이 아닌 DB의 실제 최신 권한 반영)
        String currentAuthority = user.getRole().getAuthority();

        UserDetailsImpl userDetails = new UserDetailsImpl(user.getId(), user.getEmail(), currentAuthority);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("[JWT Auth] 인증 성공 - userId={}, email={}, role={}", user.getId(), user.getEmail(), currentAuthority);
    }
}
