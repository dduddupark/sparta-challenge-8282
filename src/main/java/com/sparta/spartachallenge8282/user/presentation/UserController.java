package com.sparta.spartachallenge8282.user.presentation;

import com.sparta.spartachallenge8282.global.common.ApiResponse;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import com.sparta.spartachallenge8282.user.presentation.dto.request.UpdateUserRequest;
import com.sparta.spartachallenge8282.user.presentation.dto.request.ChangePasswordRequest;
import com.sparta.spartachallenge8282.user.presentation.dto.response.UserResponse;
import com.sparta.spartachallenge8282.user.application.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 내 정보 조회 */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyInfo(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        UserResponse data = userService.getMyInfo(userDetails.userId());
        return ResponseEntity.ok(ApiResponse.success("회원정보 조회 성공", data));
    }

    /** 내 정보 수정 (닉네임, 주소) */
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyInfo(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody UpdateUserRequest request) {
        UserResponse data = userService.updateMyInfo(userDetails.userId(), request);
        return ResponseEntity.ok(ApiResponse.success("회원정보 수정 완료", data));
    }

    /** 비밀번호 변경 */
    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userDetails.userId(), request);
        return ResponseEntity.ok(ApiResponse.success("비밀번호 변경 완료"));
    }

    /** 회원 탈퇴 */
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        userService.withdraw(userDetails.userId());
        return ResponseEntity.ok(ApiResponse.success("회원 탈퇴 완료"));
    }
}
