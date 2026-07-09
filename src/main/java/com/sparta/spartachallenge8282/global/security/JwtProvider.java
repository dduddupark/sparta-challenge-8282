package com.sparta.spartachallenge8282.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 액세스/리프레시 토큰 생성·파싱·검증 유틸리티
 * <p>
 * iat : 발급 시간(Issued At)
 * exp : 만료 시간(Expiration)
 * <p>
 * 액세스 토큰 payload
 * {
 * sub : email,
 * userId : 1,
 * role : "ROLE_CUSTOMER",
 * iat,
 * exp
 * }
 * <p>
 * 리프레시 토큰 payload
 * {
 * sub : email,
 * iat,
 * exp
 * }
 * <p>
 * email은 JWT Subject(sub)에 저장되며,
 * userId는 서버 내부 처리(AuditorAware 등)를 위해 별도 Claim으로 저장한다.
 */
@Slf4j
@Component
public class JwtProvider {

    public static final String BEARER_PREFIX = "Bearer ";

    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_ROLE = "role";

    private final SecretKey secretKey;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration-ms:3600000}") long accessExpirationMs,
            @Value("${jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    // ── 토큰 생성 ──────────────────────────────────────────────────────────────

    /**
     * 액세스 토큰 생성 (Bearer 접두사 포함).
     *
     * @param userId 사용자 PK (AuditorAware 등 내부 사용)
     * @param email  사용자 아이디 (subject, PRD 스펙)
     * @param role   역할 문자열 (ex. "ROLE_CUSTOMER")
     */
    public String createAccessToken(Long userId, String email, String role) {
        Date now = new Date();
        String token = Jwts.builder()
                .subject(email)
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_ROLE, role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessExpirationMs))
                .signWith(secretKey)
                .compact();
        return BEARER_PREFIX + token;
    }

    /**
     * 리프레시 토큰 생성 (Bearer 접두사 없음, DB에 저장).
     * userId·role 미포함 — 재발급 시 email으로 DB 조회 후 최신 정보 반영.
     *
     * @param email 사용자 아이디 (subject)
     */
    public String createRefreshToken(String email) {
        Date now = new Date();
        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshExpirationMs))
                .signWith(secretKey)
                .compact();
    }

    // ── 토큰 파싱 ──────────────────────────────────────────────────────────────

    /**
     * Authorization 헤더에서 "Bearer " 접두사를 제거하고 순수 토큰 반환.
     * 접두사가 없으면 null 반환.
     */
    public String resolveToken(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * 토큰 파싱 → Claims 반환 (만료된 토큰은 ExpiredJwtException 발생)
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ── 토큰 검증 ──────────────────────────────────────────────────────────────

    /**
     * 토큰 유효성 검증 (서명 + 만료 여부).
     * 액세스 토큰 필터에서 사용.
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("[JWT] 만료된 토큰: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("[JWT] 유효하지 않은 토큰: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 리프레시 토큰 유효성 검증.
     * 만료·변조 여부를 확인하고 subject(email)를 반환한다.
     * 유효하지 않으면 null 반환.
     *
     * @param refreshToken DB에 저장된 리프레시 토큰 원문
     */
    public String validateRefreshToken(String refreshToken) {
        try {
            return parseClaims(refreshToken).getSubject();
        } catch (ExpiredJwtException e) {
            log.warn("[JWT] 만료된 리프레시 토큰");
        } catch (JwtException e) {
            log.warn("[JWT] 유효하지 않은 리프레시 토큰: {}", e.getMessage());
        }
        return null;
    }

    // ── Claims 추출 헬퍼 ───────────────────────────────────────────────────────

    public String getEmailFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public Long getUserIdFromToken(String token) {
        return parseClaims(token).get(CLAIM_USER_ID, Long.class);
    }

    public String getRoleFromToken(String token) {
        return parseClaims(token).get(CLAIM_ROLE, String.class);
    }
}
