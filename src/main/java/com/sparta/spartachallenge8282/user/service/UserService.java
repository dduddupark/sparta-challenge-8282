package com.sparta.spartachallenge8282.user.service;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.global.security.JwtProvider;
import com.sparta.spartachallenge8282.user.entity.User;
import com.sparta.spartachallenge8282.user.entity.UserRole;
import com.sparta.spartachallenge8282.user.repository.UserRepository;
import com.sparta.spartachallenge8282.user.presentation.dto.request.LoginRequest;
import com.sparta.spartachallenge8282.user.presentation.dto.request.SignUpRequest;
import com.sparta.spartachallenge8282.user.presentation.dto.response.LoginResponse;
import com.sparta.spartachallenge8282.user.presentation.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    // ── 1. 회원가입 ─────────────────────────────────────────────────────────────

    /**
     * 회원가입.
     * 기본 CUSTOMER 권한으로 고정 가입되며, 중복 이메일 가입 요청 시 에러를 반환한다.
     */
    @Transactional
    public UserResponse signup(SignUpRequest request) {
        if (userRepository.existsByEmailAndDeletedAtIsNull(request.email())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        User user = User.builder()
                .email(request.email())
                .password(encodedPassword)
                .nickname(request.nickname())
                .address(request.address())
                .role(UserRole.CUSTOMER)
                .build();

        User savedUser = userRepository.save(user);
        log.info("[Signup] 회원가입 완료. id={}, email={}", savedUser.getId(), savedUser.getEmail());
        return UserResponse.from(savedUser);
    }

    // ── 2. 로그인 / 로그아웃 / 토큰 재발급 ─────────────────────────────────────

    /**
     * 로그인.
     * 이메일 미존재·비밀번호 불일치 에러 메시지를 통합하여 이메일 수집 공격을 방어한다.
     * 로그인 성공 시 AccessToken + RefreshToken을 발급하고, RefreshToken을 DB에 저장한다.
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(request.email())
                .orElseGet(() -> {
                    log.warn("[Login] 로그인 실패 - 존재하지 않는 이메일. email={}", request.email());
                    throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
                });

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            log.warn("[Login] 로그인 실패 - 비밀번호 불일치. userId={}", user.getId());
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken  = jwtProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole().getAuthority());
        String refreshToken = jwtProvider.createRefreshToken(user.getEmail());

        user.updateRefreshToken(refreshToken);
        log.info("[Login] 로그인 성공. id={}, email={}", user.getId(), user.getEmail());
        return new LoginResponse(accessToken, refreshToken);
    }

    /**
     * 로그아웃.
     * DB에 저장된 RefreshToken을 즉시 삭제하여 재사용을 차단한다.
     */
    @Transactional
    public void logout(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        user.clearRefreshToken();
        log.info("[Logout] 로그아웃 완료. id={}", userId);
    }

    /**
     * 토큰 재발급 (RTR - Refresh Token Rotation).
     * RefreshToken 검증 후 AccessToken + RefreshToken을 새로 발급하고 DB를 갱신한다.
     * 토큰 재사용 공격을 방어하기 위해 RefreshToken도 매번 교체한다.
     */
    @Transactional
    public LoginResponse reissue(String refreshToken) {
        String email = jwtProvider.validateRefreshToken(refreshToken);
        if (email == null) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // DB에 저장된 RefreshToken과 일치 여부 확인 (탈취 토큰 차단)
        if (!refreshToken.equals(user.getRefreshToken())) {
            user.clearRefreshToken(); // 불일치 시 즉시 무효화
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        String newAccessToken  = jwtProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole().getAuthority());
        String newRefreshToken = jwtProvider.createRefreshToken(user.getEmail());

        user.updateRefreshToken(newRefreshToken);
        log.info("[Reissue] 토큰 재발급 완료. id={}", user.getId());
        return new LoginResponse(newAccessToken, newRefreshToken);
    }
}
