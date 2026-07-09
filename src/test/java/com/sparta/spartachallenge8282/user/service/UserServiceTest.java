package com.sparta.spartachallenge8282.user.service;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.global.security.JwtProvider;
import com.sparta.spartachallenge8282.user.entity.User;
import com.sparta.spartachallenge8282.user.entity.UserRole;
import com.sparta.spartachallenge8282.user.repository.UserRepository;
import com.sparta.spartachallenge8282.user.presentation.dto.request.SignUpRequest;
import com.sparta.spartachallenge8282.user.presentation.dto.response.UserResponse;
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
}
