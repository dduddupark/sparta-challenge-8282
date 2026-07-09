package com.sparta.spartachallenge8282.user.service;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.global.security.JwtProvider;
import com.sparta.spartachallenge8282.user.entity.User;
import com.sparta.spartachallenge8282.user.entity.UserRole;
import com.sparta.spartachallenge8282.user.repository.UserRepository;
import com.sparta.spartachallenge8282.user.presentation.dto.request.SignUpRequest;
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
}
