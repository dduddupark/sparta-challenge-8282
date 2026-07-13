package com.sparta.spartachallenge8282.user.service;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.global.security.JwtProvider;
import com.sparta.spartachallenge8282.user.dto.request.*;
import com.sparta.spartachallenge8282.user.dto.response.LoginResponse;
import com.sparta.spartachallenge8282.user.dto.response.UserResponse;
import com.sparta.spartachallenge8282.user.entity.User;
import com.sparta.spartachallenge8282.user.entity.UserRole;
import com.sparta.spartachallenge8282.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 단위 테스트")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private UserService userService;

    // ── 회원가입 ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("회원가입 (signup)")
    class SignupTest {

        private SignUpRequest createRequest(String email) {
            return new SignUpRequest(
                    email,
                    "Password123!",
                    "코딩하는토끼",
                    "서울시 강남구"
            );
        }

        @Test
        @DisplayName("정상 회원가입 성공")
        void signup_success() {
            // given
            SignUpRequest request = createRequest("test@sparta.com");

            given(userRepository.existsByEmailAndDeletedAtIsNull(request.email()))
                    .willReturn(false);
            given(passwordEncoder.encode(request.password()))
                    .willReturn("encodedPassword");

            User savedUser = User.builder()
                    .email(request.email())
                    .password("encodedPassword")
                    .nickname(request.nickname())
                    .address(request.address())
                    .role(UserRole.CUSTOMER)
                    .build();
            ReflectionTestUtils.setField(savedUser, "id", 1L);
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            // when
            UserResponse result = userService.signup(request);

            // then
            assertThat(result.email()).isEqualTo("test@sparta.com");
            assertThat(result.nickname()).isEqualTo("코딩하는토끼");
            assertThat(result.role()).isEqualTo(UserRole.CUSTOMER);
            verify(userRepository).save(any(User.class));
            verify(passwordEncoder).encode("Password123!");
        }

        @Test
        @DisplayName("이미 가입된 이메일 - DUPLICATE_EMAIL 예외 발생")
        void signup_duplicateEmail_throwsException() {
            // given
            SignUpRequest request = createRequest("duplicate@sparta.com");
            given(userRepository.existsByEmailAndDeletedAtIsNull(request.email()))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.signup(request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.DUPLICATE_EMAIL));

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("비밀번호가 BCrypt로 암호화되어 저장됨")
        void signup_passwordIsEncoded() {
            // given
            SignUpRequest request = createRequest("test@sparta.com");
            given(userRepository.existsByEmailAndDeletedAtIsNull(anyString())).willReturn(false);
            given(passwordEncoder.encode("Password123!")).willReturn("$2a$10$encodedHash");

            User savedUser = User.builder()
                    .email(request.email())
                    .password("$2a$10$encodedHash")
                    .nickname(request.nickname())
                    .address(request.address())
                    .role(UserRole.CUSTOMER)
                    .build();
            ReflectionTestUtils.setField(savedUser, "id", 1L);
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            // when
            userService.signup(request);

            // then
            verify(passwordEncoder).encode("Password123!");
        }

        @Test
        @DisplayName("회원가입 시 role은 항상 CUSTOMER로 고정됨")
        void signup_roleIsAlwaysCustomer() {
            // given
            SignUpRequest request = createRequest("test@sparta.com");
            given(userRepository.existsByEmailAndDeletedAtIsNull(anyString())).willReturn(false);
            given(passwordEncoder.encode(anyString())).willReturn("encoded");

            User savedUser = User.builder()
                    .email(request.email())
                    .password("encoded")
                    .nickname(request.nickname())
                    .address(request.address())
                    .role(UserRole.CUSTOMER)
                    .build();
            ReflectionTestUtils.setField(savedUser, "id", 1L);
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            // when
            UserResponse result = userService.signup(request);

            // then
            assertThat(result.role()).isEqualTo(UserRole.CUSTOMER);
        }
    }

    // ── 로그인 / 로그아웃 / 토큰 재발급 ────────────────────────────────────────

    @Nested
    @DisplayName("로그인 (login)")
    class LoginTest {

        private User makeUser() {
            User user = User.builder()
                    .email("test@sparta.com")
                    .password("encodedPassword")
                    .nickname("코딩하는토끼")
                    .address("서울시 강남구")
                    .role(UserRole.CUSTOMER)
                    .build();
            ReflectionTestUtils.setField(user, "id", 1L);
            return user;
        }

        @Test
        @DisplayName("정상 로그인 성공 - AccessToken + RefreshToken 반환")
        void login_success() {
            // given
            LoginRequest request = new LoginRequest("test@sparta.com", "Password123!");
            User user = makeUser();

            given(userRepository.findByEmailAndDeletedAtIsNull(request.email()))
                    .willReturn(java.util.Optional.of(user));
            given(passwordEncoder.matches(request.password(), user.getPassword()))
                    .willReturn(true);
            given(jwtProvider.createAccessToken(user.getId(), user.getEmail(), "ROLE_CUSTOMER"))
                    .willReturn("accessToken");
            given(jwtProvider.createRefreshToken(user.getEmail()))
                    .willReturn("refreshToken");

            // when
            LoginResponse result = userService.login(request);

            // then
            assertThat(result.accessToken()).isEqualTo("accessToken");
            assertThat(result.refreshToken()).isEqualTo("refreshToken");
            verify(userRepository).findByEmailAndDeletedAtIsNull(request.email());
        }

        @Test
        @DisplayName("존재하지 않는 이메일 - INVALID_CREDENTIALS 예외 발생")
        void login_emailNotFound_throwsException() {
            // given
            LoginRequest request = new LoginRequest("none@sparta.com", "Password123!");
            given(userRepository.findByEmailAndDeletedAtIsNull(request.email()))
                    .willReturn(java.util.Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_PASSWORD));
        }

        @Test
        @DisplayName("비밀번호 불일치 - INVALID_CREDENTIALS 예외 발생")
        void login_wrongPassword_throwsException() {
            // given
            LoginRequest request = new LoginRequest("test@sparta.com", "wrongPassword!");
            User user = makeUser();
            given(userRepository.findByEmailAndDeletedAtIsNull(request.email()))
                    .willReturn(java.util.Optional.of(user));
            given(passwordEncoder.matches(request.password(), user.getPassword()))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_PASSWORD));
        }
    }

    @Nested
    @DisplayName("로그아웃 (logout)")
    class LogoutTest {

        @Test
        @DisplayName("정상 로그아웃 - RefreshToken 삭제")
        void logout_success() {
            // given
            User user = User.builder()
                    .email("test@sparta.com").password("encoded")
                    .nickname("토끼").address("서울").role(UserRole.CUSTOMER).build();
            ReflectionTestUtils.setField(user, "id", 1L);
            user.updateRefreshToken("someRefreshToken");

            given(userRepository.findByIdAndDeletedAtIsNull(1L))
                    .willReturn(java.util.Optional.of(user));

            // when
            userService.logout(1L);

            // then
            assertThat(user.getRefreshToken()).isNull();
        }
    }

    @Nested
    @DisplayName("토큰 재발급 (reissue)")
    class ReissueTest {

        @Test
        @DisplayName("정상 재발급 - 새 AccessToken + RefreshToken 반환")
        void reissue_success() {
            // given
            String oldRefreshToken = "validRefreshToken";
            User user = User.builder()
                    .email("test@sparta.com").password("encoded")
                    .nickname("토끼").address("서울").role(UserRole.CUSTOMER).build();
            ReflectionTestUtils.setField(user, "id", 1L);
            user.updateRefreshToken(oldRefreshToken);

            given(jwtProvider.resolveToken(oldRefreshToken)).willReturn(oldRefreshToken);
            given(jwtProvider.validateRefreshToken(oldRefreshToken)).willReturn("test@sparta.com");
            given(userRepository.findByEmailAndDeletedAtIsNullForUpdate("test@sparta.com"))
                    .willReturn(java.util.Optional.of(user));
            given(jwtProvider.createAccessToken(1L, "test@sparta.com", "ROLE_CUSTOMER"))
                    .willReturn("newAccessToken");
            given(jwtProvider.createRefreshToken("test@sparta.com"))
                    .willReturn("newRefreshToken");

            // when
            LoginResponse result = userService.reissue(oldRefreshToken);

            // then
            assertThat(result.accessToken()).isEqualTo("newAccessToken");
            assertThat(result.refreshToken()).isEqualTo("newRefreshToken");
            assertThat(user.getRefreshToken()).isEqualTo("newRefreshToken"); // DB 갱신 확인
        }

        @Test
        @DisplayName("유효하지 않은 RefreshToken - INVALID_TOKEN 예외 발생")
        void reissue_invalidToken_throwsException() {
            // given
            given(jwtProvider.resolveToken("invalidToken")).willReturn("invalidToken");
            given(jwtProvider.validateRefreshToken("invalidToken")).willReturn(null);

            // when & then
            assertThatThrownBy(() -> userService.reissue("invalidToken"))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));
        }

        @Test
        @DisplayName("탈취된 RefreshToken - DB 불일치 시 토큰 무효화 후 예외 발생")
        void reissue_stolenToken_clearsAndThrows() {
            // given
            String stolenToken = "stolenRefreshToken";
            User user = User.builder()
                    .email("test@sparta.com").password("encoded")
                    .nickname("토끼").address("서울").role(UserRole.CUSTOMER).build();
            ReflectionTestUtils.setField(user, "id", 1L);
            user.updateRefreshToken("validToken");

            given(jwtProvider.resolveToken(stolenToken)).willReturn(stolenToken);
            given(jwtProvider.validateRefreshToken(stolenToken)).willReturn("test@sparta.com");
            given(userRepository.findByEmailAndDeletedAtIsNullForUpdate("test@sparta.com"))
                    .willReturn(java.util.Optional.of(user));

            // when & then
            assertThatThrownBy(() -> userService.reissue(stolenToken))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));
        }
    }

    // ── 회원 정보 조회 / 수정 ───────────────────────────────────────────────

    @Nested
    @DisplayName("회원정보 조회 (getMyInfo)")
    class GetMyInfoTest {

        @Test
        @DisplayName("정상 조회 성공")
        void getMyInfo_success() {
            // given
            User user = User.builder()
                    .email("test@sparta.com").password("encoded")
                    .nickname("코딩하는토끼").address("서울시 강남구").role(UserRole.CUSTOMER).build();
            ReflectionTestUtils.setField(user, "id", 1L);

            given(userRepository.findByIdAndDeletedAtIsNull(1L))
                    .willReturn(java.util.Optional.of(user));

            // when
            UserResponse result = userService.getMyInfo(1L);

            // then
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.email()).isEqualTo("test@sparta.com");
            assertThat(result.nickname()).isEqualTo("코딩하는토끼");
        }

        @Test
        @DisplayName("탈퇴한 회원 조회 - USER_NOT_FOUND 예외 발생")
        void getMyInfo_deletedUser_throwsException() {
            // given
            given(userRepository.findByIdAndDeletedAtIsNull(99L))
                    .willReturn(java.util.Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.getMyInfo(99L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.USER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("회원정보 수정 (updateMyInfo)")
    class UpdateMyInfoTest {

        @Test
        @DisplayName("니키네임 + 주소 수정 성공")
        void updateMyInfo_success() {
            // given
            User user = User.builder()
                    .email("test@sparta.com").password("encoded")
                    .nickname("오래된닉네임").address("오래된주소").role(UserRole.CUSTOMER).build();
            ReflectionTestUtils.setField(user, "id", 1L);

            given(userRepository.findByIdAndDeletedAtIsNull(1L))
                    .willReturn(java.util.Optional.of(user));

            UpdateUserRequest request = new UpdateUserRequest("새닉네임", "새주소");

            // when
            UserResponse result = userService.updateMyInfo(1L, request);

            // then
            assertThat(result.nickname()).isEqualTo("새닉네임");
            assertThat(result.address()).isEqualTo("새주소");
        }

        @Test
        @DisplayName("null 요청 시 기존 값 유지")
        void updateMyInfo_nullRequest_keepsOriginal() {
            // given
            User user = User.builder()
                    .email("test@sparta.com").password("encoded")
                    .nickname("원래닉네임").address("원래주소").role(UserRole.CUSTOMER).build();
            ReflectionTestUtils.setField(user, "id", 1L);

            given(userRepository.findByIdAndDeletedAtIsNull(1L))
                    .willReturn(java.util.Optional.of(user));

            UpdateUserRequest request = new UpdateUserRequest(null, null); // null 요청

            // when
            UserResponse result = userService.updateMyInfo(1L, request);

            // then
            assertThat(result.nickname()).isEqualTo("원래닉네임"); // 기존 값 유지
            assertThat(result.address()).isEqualTo("원래주소");   // 기존 값 유지
        }

        @Test
        @DisplayName("이메일·role은 수정 불가 (응답에 변경 없음)&quot;")
        void updateMyInfo_emailAndRoleNotChanged() {
            // given
            User user = User.builder()
                    .email("test@sparta.com").password("encoded")
                    .nickname("토끼").address("서울").role(UserRole.CUSTOMER).build();
            ReflectionTestUtils.setField(user, "id", 1L);

            given(userRepository.findByIdAndDeletedAtIsNull(1L))
                    .willReturn(java.util.Optional.of(user));

            UpdateUserRequest request = new UpdateUserRequest("새닉네임", "새주소");

            // when
            UserResponse result = userService.updateMyInfo(1L, request);

            // then
            assertThat(result.email()).isEqualTo("test@sparta.com"); // 이메일 불변
            assertThat(result.role()).isEqualTo(UserRole.CUSTOMER);   // role 불변
        }
    }

    // ── 4. 비밀번호 변경 ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("비밀번호 변경 (changePassword)")
    class ChangePasswordTest {

        @Test
        @DisplayName("정상 비밀번호 변경 성공")
        void changePassword_success() {
            // given
            User user = User.builder()
                    .email("test@sparta.com").password("oldPassword")
                    .nickname("토끼").address("서울").role(UserRole.CUSTOMER).build();
            ReflectionTestUtils.setField(user, "id", 1L);

            given(userRepository.findByIdAndDeletedAtIsNull(1L))
                    .willReturn(java.util.Optional.of(user));

            ChangePasswordRequest request = new ChangePasswordRequest("oldPassword", "NewPassword123!");

            given(passwordEncoder.matches("oldPassword", "oldPassword")).willReturn(true);
            given(passwordEncoder.matches("NewPassword123!", "oldPassword")).willReturn(false);
            given(passwordEncoder.encode("NewPassword123!")).willReturn("encodedNewPassword");

            // when
            userService.changePassword(1L, request);

            // then
            verify(passwordEncoder).encode("NewPassword123!");
            assertThat(user.getPassword()).isEqualTo("encodedNewPassword");
        }

        @Test
        @DisplayName("현재 비밀번호 불일치 - INVALID_PASSWORD 예외 발생")
        void changePassword_wrongCurrentPassword_throwsException() {
            // given
            User user = User.builder()
                    .email("test@sparta.com").password("oldPassword")
                    .nickname("토끼").address("서울").role(UserRole.CUSTOMER).build();
            ReflectionTestUtils.setField(user, "id", 1L);

            given(userRepository.findByIdAndDeletedAtIsNull(1L))
                    .willReturn(java.util.Optional.of(user));

            ChangePasswordRequest request = new ChangePasswordRequest("wrongPassword", "NewPassword123!");

            given(passwordEncoder.matches("wrongPassword", "oldPassword")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> userService.changePassword(1L, request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_PASSWORD));
        }

        @Test
        @DisplayName("새 비밀번호가 기존과 동일함 - SAME_AS_OLD_PASSWORD 예외 발생")
        void changePassword_sameAsOldPassword_throwsException() {
            // given
            User user = User.builder()
                    .email("test@sparta.com").password("oldPassword")
                    .nickname("토끼").address("서울").role(UserRole.CUSTOMER).build();
            ReflectionTestUtils.setField(user, "id", 1L);

            given(userRepository.findByIdAndDeletedAtIsNull(1L))
                    .willReturn(java.util.Optional.of(user));

            ChangePasswordRequest request = new ChangePasswordRequest("oldPassword", "oldPassword");

            given(passwordEncoder.matches("oldPassword", "oldPassword")).willReturn(true); // 현재 비밀번호 일치 (첫번째 검증)
            // 두번째 검증 (새 비밀번호 == 기존 비밀번호) - 같은 matches() 호출이므로 두 번 연속 true 반환하도록 설정
            given(passwordEncoder.matches("oldPassword", "oldPassword")).willReturn(true, true);

            // when & then
            assertThatThrownBy(() -> userService.changePassword(1L, request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.DUPLICATE_PASSWORD));
        }
    }

    // ── 5. 회원 탈퇴 ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("회원 탈퇴 (withdraw)")
    class WithdrawTest {

        @Test
        @DisplayName("정상 회원 탈퇴 - Soft Delete 적용 및 RefreshToken 삭제")
        void withdraw_success() {
            // given
            User user = User.builder()
                    .email("test@sparta.com").password("encoded")
                    .nickname("토끼").address("서울").role(UserRole.CUSTOMER).build();
            ReflectionTestUtils.setField(user, "id", 1L);
            user.updateRefreshToken("someRefreshToken");

            given(userRepository.findByIdAndDeletedAtIsNull(1L))
                    .willReturn(java.util.Optional.of(user));

            // when
            userService.withdraw(1L);

            // then
            assertThat(user.isDeleted()).isTrue();
            assertThat(user.getDeletedAt()).isNotNull();
            assertThat(user.getDeletedBy()).isEqualTo(1L);
            assertThat(user.getRefreshToken()).isNull(); // RefreshToken 삭제 확인
        }

        @Test
        @DisplayName("이미 탈퇴한 회원 - USER_NOT_FOUND 예외 발생")
        void withdraw_alreadyDeleted_throwsException() {
            // given
            given(userRepository.findByIdAndDeletedAtIsNull(1L))
                    .willReturn(java.util.Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.withdraw(1L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.USER_NOT_FOUND));
        }

        @Test
        @DisplayName("MASTER 권한을 가진 회원은 탈퇴할 수 없음 - MASTER_CANNOT_BE_DELETED 예외 발생")
        void withdraw_master_throwsException() {
            // given
            User user = User.builder()
                    .email("master@sparta.com").password("encoded")
                    .nickname("마스터").address("서울").role(UserRole.MASTER).build();
            ReflectionTestUtils.setField(user, "id", 1L);

            given(userRepository.findByIdAndDeletedAtIsNull(1L))
                    .willReturn(java.util.Optional.of(user));

            // when & then
            assertThatThrownBy(() -> userService.withdraw(1L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.MASTER_CANNOT_BE_DELETED));
        }
    }

    @Nested
    @DisplayName("회원 강제 삭제 (deleteUser)")
    class DeleteUserTest {

        @Test
        @DisplayName("MASTER 권한을 가진 회원은 강제 삭제할 수 없음 - MASTER_CANNOT_BE_DELETED 예외 발생")
        void deleteUser_master_throwsException() {
            // given
            User targetUser = User.builder()
                    .email("master2@sparta.com").password("encoded")
                    .nickname("마스터2").address("서울").role(UserRole.MASTER).build();
            ReflectionTestUtils.setField(targetUser, "id", 2L);

            given(userRepository.findByIdAndDeletedAtIsNull(2L))
                    .willReturn(java.util.Optional.of(targetUser));

            // when & then
            assertThatThrownBy(() -> userService.deleteUser(2L, 1L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.MASTER_CANNOT_BE_DELETED));
        }
    }

    // ── 6. 관리자 권한 변경 ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("관리자 권한 변경 (changeRole)")
    class ChangeRoleTest {

        @Test
        @DisplayName("정상 권한 변경 성공 - MASTER가 MANAGER 부여")
        void changeRole_success() {
            // given
            User operator = User.builder()
                    .email("master@sparta.com").password("encoded")
                    .nickname("마스터").address("서울").role(UserRole.MASTER).build();
            ReflectionTestUtils.setField(operator, "id", 1L);

            User targetUser = User.builder()
                    .email("test@sparta.com").password("encoded")
                    .nickname("일반").address("서울").role(UserRole.CUSTOMER).build();
            ReflectionTestUtils.setField(targetUser, "id", 2L);

            given(userRepository.findByIdAndDeletedAtIsNull(2L)).willReturn(java.util.Optional.of(targetUser));
            given(userRepository.findByIdAndDeletedAtIsNull(2L)).willReturn(java.util.Optional.of(targetUser));

            ChangeRoleRequest request = new ChangeRoleRequest(UserRole.MANAGER);

            // when
            userService.changeRole(2L, request, UserRole.MASTER);

            // then
            assertThat(targetUser.getRole()).isEqualTo(UserRole.MANAGER);
        }

        @Test
        @DisplayName("권한 부족 - MANAGER가 MASTER 부여 시도 시 ACCESS_DENIED 예외")
        void changeRole_notMaster_throwsException() {
            User targetUser = User.builder()
                    .email("test@sparta.com").password("encoded")
                    .nickname("일반").address("서울").role(UserRole.CUSTOMER).build();
            ReflectionTestUtils.setField(targetUser, "id", 2L);

            given(userRepository.findByIdAndDeletedAtIsNull(2L)).willReturn(java.util.Optional.of(targetUser));

            ChangeRoleRequest request = new ChangeRoleRequest(UserRole.MASTER);

            // when & then
            assertThatThrownBy(() -> userService.changeRole(2L, request, UserRole.MANAGER))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.ACCESS_DENIED));
        }

        @Test
        @DisplayName("탈퇴한 회원 대상 권한 변경 시도 시 USER_NOT_FOUND 예외")
        void changeRole_targetDeleted_throwsException() {
            // given
            given(userRepository.findByIdAndDeletedAtIsNull(2L)).willReturn(java.util.Optional.empty());

            ChangeRoleRequest request = new ChangeRoleRequest(UserRole.MANAGER);

            // when & then
            assertThatThrownBy(() -> userService.changeRole(2L, request, UserRole.MASTER))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.USER_NOT_FOUND));
        }

        @Test
        @DisplayName("MASTER 권한 부여 시 이미 MASTER가 존재하면 ALREADY_EXISTS_MASTER 예외 발생")
        void changeRole_grantMaster_alreadyExists_throwsException() {
            // given
            User targetUser = User.builder()
                    .email("test@sparta.com").password("encoded")
                    .nickname("일반").address("서울").role(UserRole.CUSTOMER).build();
            ReflectionTestUtils.setField(targetUser, "id", 2L);

            given(userRepository.findByIdAndDeletedAtIsNull(2L)).willReturn(java.util.Optional.of(targetUser));
            given(userRepository.existsByRoleAndDeletedAtIsNull(UserRole.MASTER)).willReturn(true);

            ChangeRoleRequest request = new ChangeRoleRequest(UserRole.MASTER);

            // when & then
            assertThatThrownBy(() -> userService.changeRole(2L, request, UserRole.MASTER))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.ALREADY_EXISTS_MASTER));
        }
    }
}
