package com.sparta.spartachallenge8282.global.controller;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.global.security.JwtProvider;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import com.sparta.spartachallenge8282.user.dto.response.UserResponse;
import com.sparta.spartachallenge8282.user.entity.User;
import com.sparta.spartachallenge8282.user.repository.UserRepository;
import com.sparta.spartachallenge8282.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 개발/테스트 전용 컨트롤러.
 *
 * <p><b>⚠️ 주의: 프로젝트 제출 전 반드시 삭제하거나 @Profile("dev")로 비활성화할 것.</b>
 */
// @Profile("dev") // 운영 환경 배포 시 이 주석을 해제하여 dev 프로필에서만 활성화
@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class TestController {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    /**
     * PK로 유저 정보 + 토큰 직접 조회 (토큰 불필요).
     *
     * <pre>
     * GET /api/v1/test/{userId}
     * ex) GET /api/v1/test/3
     *
     * Response:
     * {
     *   "userId": 3,
     *   "email": "user@test.com",
     *   "role": "CUSTOMER",
     *   "nickname": "...",
     *   "address": "...",
     *   "accessToken": "eyJhbGci...",
     *   "refreshToken": "eyJhbGci..." (DB에 저장된 값, 없으면 null)
     * }
     * </pre>
     */
    @GetMapping("/{userId}")
    public Map<String, Object> checkUser(@PathVariable Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 해당 유저의 accessToken 즉석 발급
        String accessToken = jwtProvider.createAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().getAuthority()
        );

        // 순서가 보장되는 LinkedHashMap 사용으로 응답 가독성 향상
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId",       user.getId());
        result.put("email",        user.getEmail());
        result.put("role",         user.getRole().name());
        result.put("nickname",     user.getNickname());
        result.put("address",      user.getAddress());
        result.put("accessToken",  accessToken);
        result.put("refreshToken", user.getRefreshToken()); // 로그인 전이면 null
        return result;
    }
}
