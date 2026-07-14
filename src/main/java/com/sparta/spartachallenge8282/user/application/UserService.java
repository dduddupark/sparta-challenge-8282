package com.sparta.spartachallenge8282.user.application;

import com.sparta.spartachallenge8282.global.common.PageResponse;
import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.global.security.JwtProvider;
import com.sparta.spartachallenge8282.user.presentation.dto.request.*;
import com.sparta.spartachallenge8282.user.presentation.dto.response.LoginResponse;
import com.sparta.spartachallenge8282.user.presentation.dto.response.UserResponse;
import com.sparta.spartachallenge8282.user.domain.User;
import com.sparta.spartachallenge8282.user.domain.UserRole;
import com.sparta.spartachallenge8282.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User 도메인 서비스 레이어.
 * 회원 관련 비즈니스 규칙 및 트랜잭션 경계를 담당한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    // ── 1. 인증(Auth) 비즈니스 로직 ─────────────────────────────────────────────

    /**
     * 회원 가입.
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
                .role(UserRole.CUSTOMER) // 가입 시 CUSTOMER 고정
                .build();

        User savedUser = userRepository.save(user);
        log.info("[Signup] 회원가입 완료. id={}, email={}", savedUser.getId(), savedUser.getEmail());
        return UserResponse.from(savedUser);
    }

    /**
     * 로그인.
     * 보안 침해(이메일 탐색 공격)를 막기 위해 가입되지 않은 이메일 조회 시에도
     * 비밀번호 오류와 동일한 INVALID_PASSWORD 예외를 응답한다.
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_PASSWORD)); // 에러 메시지 통합

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        String accessToken = jwtProvider.createAccessToken(
                user.getId(), user.getEmail(), user.getRole().getAuthority(), user.getTokenVersion());
        String refreshToken = jwtProvider.createRefreshToken(user.getEmail());

        user.updateRefreshToken(refreshToken); // DB에 Refresh Token 캐싱

        log.info("[Login] 로그인 성공. userId={}, email={}", user.getId(), user.getEmail());
        return new LoginResponse(accessToken, refreshToken);
    }

    /**
     * 로그아웃.
     * Refresh Token을 제거하고 기존 Access Token을 모두 무효화한다.
     */
    @Transactional
    public void logout(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.invalidateTokens();
        log.info("[Logout] 로그아웃 완료. userId={}", userId);
    }

    /**
     * Access Token 재발급
     * 전달된 Refresh Token의 만료/유효성 및 DB 값 일치 여부를 검증한 후 신규 Access Token을 반환한다.
     */
    @Transactional
    public LoginResponse reissue(String bearerRefreshToken) {
        String token = jwtProvider.resolveToken(bearerRefreshToken);
        if (token == null) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String email = jwtProvider.validateRefreshToken(token);
        if (email == null) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        User user = userRepository.findByEmailAndDeletedAtIsNullForUpdate(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // DB에 기록된 토큰과 다른 경우 (중복 재발급 혹은 탈취 가능성 차단)
        if (user.getRefreshToken() == null || !user.getRefreshToken().equals(token)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 새로운 Access Token 생성 및 Refresh Token Rotation(RTR) 적용
        String newAccessToken = jwtProvider.createAccessToken(
                user.getId(), user.getEmail(), user.getRole().getAuthority(), user.getTokenVersion());
        String newRefreshToken = jwtProvider.createRefreshToken(user.getEmail());

        user.updateRefreshToken(newRefreshToken);

        log.info("[Reissue] 토큰 재발급 완료. userId={}", user.getId());
        return new LoginResponse(newAccessToken, newRefreshToken);
    }

    // ── 2. 회원(User) 정보 관리 ────────────────────────────────────────────────

    /**
     * 내 정보 조회.
     */
    public UserResponse getMyInfo(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return UserResponse.from(user);
    }

    /**
     * 회원 정보 수정 (닉네임, 주소).
     * DTO 값이 null 이거나 비어 있는 경우 덮어쓰지 않도록 엔티티의 보호 가드가 작동한다.
     */
    @Transactional
    public UserResponse updateMyInfo(Long userId, UpdateUserRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.updateProfile(request.nickname(), request.address());
        log.info("[Profile Update] 정보 변경 완료. userId={}", userId);
        return UserResponse.from(user);
    }

    /**
     * 비밀번호 변경.
     * 현재 비밀번호와 동일한 새 비밀번호로의 변경 시도를 차단한다.
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 현재 비밀번호 검증
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        // 동일 비밀번호 재사용 방지 검증
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.DUPLICATE_PASSWORD);
        }

        user.updatePassword(passwordEncoder.encode(request.newPassword()));
        user.invalidateTokens();
        log.info("[Password Change] 비밀번호 변경 완료. userId={}", userId);
    }

    /**
     * 회원 탈퇴 (Soft Delete).
     * 로그아웃과 탈퇴 처리를 동시에 진행하여 세션을 즉각 무효화한다.
     */
    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getRole() == UserRole.MASTER) {
            throw new CustomException(ErrorCode.MASTER_CANNOT_BE_DELETED);
        }

        user.softDelete(userId);
        user.invalidateTokens();
        log.info("[User Withdraw] 회원 탈퇴 처리 완료. userId={}", userId);
    }

    // ── 3. 관리자(Manager) 비즈니스 로직 ─────────────────────────────────────────

    /**
     * [관리자용] 활성 회원 목록 조회 (페이징).
     */
    public PageResponse<UserResponse> getActiveUsers(Pageable pageable) {
        return PageResponse.from(userRepository.findAllByDeletedAtIsNull(pageable)
                .map(UserResponse::from));
    }

    /**
     * [관리자용] 탈퇴 회원 목록 조회 (페이징).
     */
    public PageResponse<UserResponse> getWithdrawnUsers(Pageable pageable) {
        return PageResponse.from(userRepository.findAllByDeletedAtIsNotNull(pageable)
                .map(UserResponse::from));
    }

    /**
     * [관리자용] 회원 상세 조회.
     * 탈퇴한 회원도 조회 가능하도록 findById 기본 메서드를 활용한다.
     */
    public UserResponse getUserDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return UserResponse.from(user);
    }

    /**
     * [관리자용] 회원 강제 삭제 (Soft Delete).
     */
    @Transactional
    public void deleteUser(Long targetUserId, Long adminUserId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getRole() == UserRole.MASTER) {
            throw new CustomException(ErrorCode.MASTER_CANNOT_BE_DELETED);
        }

        user.softDelete(adminUserId); // 삭제를 지시한 관리자 ID 기록
        user.invalidateTokens();
        log.info("[Admin Force Delete] 회원 강제 삭제 처리. targetUserId={}, adminUserId={}", targetUserId, adminUserId);
    }

    /**
     * [관리자용] 회원 권한/역할 변경.
     * 권한 상승 공격 방지 정책: MANAGER 등급은 MASTER/MANAGER 등급으로 승격/생성을 제어할 수 없다.
     * 오직 MASTER 권한을 가진 어드민만 관리자 자격(MASTER, MANAGER)을 부여할 수 있다.
     */
    @Transactional
    public String changeRole(Long targetUserId, ChangeRoleRequest request, UserRole executorRole) {
        User user = userRepository.findByIdAndDeletedAtIsNull(targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 부여하려는 역할이 MASTER인 경우, API를 통한 권한 부여는 전면 차단 (최초 DB 직접 생성)
        if (request.role() == UserRole.MASTER) {
            throw new CustomException(ErrorCode.CANNOT_ASSIGN_MASTER_ROLE);
        }

        // 부여하려는 역할이 MANAGER일 경우, 요청한 사람이 MASTER가 아닌 경우 차단
        if (request.role() == UserRole.MANAGER) {
            if (executorRole != UserRole.MASTER) {
                throw new CustomException(ErrorCode.ACCESS_DENIED);
            }
        }

        UserRole oldRole = user.getRole();
        user.updateRole(request.role());
        log.info("[Admin Role Change] 권한 변경 완료. userId={}, {} -> {} (by {})", targetUserId, oldRole, request.role(), executorRole);

        return String.format("역할 변경 완료 : %s > %s", oldRole.name(), request.role().name());
    }
}
