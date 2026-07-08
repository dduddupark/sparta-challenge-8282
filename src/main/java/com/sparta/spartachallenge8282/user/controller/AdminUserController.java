package com.sparta.spartachallenge8282.user.controller;

import com.sparta.spartachallenge8282.global.common.ApiResponse;
import com.sparta.spartachallenge8282.global.common.PageResponse;
import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import com.sparta.spartachallenge8282.user.dto.request.ChangeRoleRequest;
import com.sparta.spartachallenge8282.user.dto.response.UserResponse;
import com.sparta.spartachallenge8282.user.entity.UserRole;
import com.sparta.spartachallenge8282.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_MASTER')") // Security Authority 명세 정확히 매핑
public class AdminUserController {

    private final UserService userService;

    /**
     * 3.1 활성 회원 목록 조회 (페이징)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getActiveUsers(
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<UserResponse> data = PageResponse.from(userService.getActiveUsers(pageable));
        return ResponseEntity.ok(ApiResponse.success("회원 목록 조회 성공", data));
    }

    /**
     * 3.2 탈퇴 회원 목록 조회 (페이징)
     */
    @GetMapping("/withdrawn")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getWithdrawnUsers(
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<UserResponse> data = PageResponse.from(userService.getWithdrawnUsers(pageable));
        return ResponseEntity.ok(ApiResponse.success("탈퇴 회원 목록 조회 성공", data));
    }

    /**
     * 3.3 회원 상세 조회 (탈퇴 회원 포함)
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserDetail(@PathVariable Long userId) {
        UserResponse data = userService.getUserDetail(userId);
        return ResponseEntity.ok(ApiResponse.success("회원 상세 조회 성공", data));
    }

    /**
     * 3.4 회원 강제 삭제
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetailsImpl adminDetails) {
        userService.deleteUser(userId, adminDetails.userId());
        return ResponseEntity.ok(ApiResponse.success("회원 삭제 완료"));
    }

    /**
     * 3.5 회원 역할(권한) 변경
     */
    @PatchMapping("/{userId}/role")
    public ResponseEntity<ApiResponse<Void>> changeRole(
            @PathVariable Long userId,
            @Valid @RequestBody ChangeRoleRequest request,
            @AuthenticationPrincipal UserDetailsImpl adminDetails) {

        // "ROLE_" 접두사 유효성 검사 및 안전한 Enum 디코딩 방어 코드 적용
        String role = adminDetails.role();
        if (role == null || !role.startsWith("ROLE_")) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        String roleCode = role.substring(5);
        UserRole executorRole = UserRole.valueOf(roleCode);

        userService.changeRole(userId, request, executorRole);
        return ResponseEntity.ok(ApiResponse.success("역할 변경 완료"));
    }
}
