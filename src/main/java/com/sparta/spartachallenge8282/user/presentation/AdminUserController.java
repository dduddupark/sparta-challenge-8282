package com.sparta.spartachallenge8282.user.presentation;

import com.sparta.spartachallenge8282.global.common.ApiResponse;
import com.sparta.spartachallenge8282.global.common.PageResponse;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import com.sparta.spartachallenge8282.user.presentation.dto.request.ChangeRoleRequest;
import com.sparta.spartachallenge8282.user.presentation.dto.response.UserResponse;
import com.sparta.spartachallenge8282.user.domain.UserRole;
import com.sparta.spartachallenge8282.user.application.UserService;
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
@PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_MASTER')") // 시큐리티 Authority 명세 정확히 매핑
public class AdminUserController {

    private final UserService userService;

    /** 3.1 활성 회원 목록 조회 (페이징) */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getActiveUsers(
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<UserResponse> data = userService.getActiveUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success("회원 목록 조회 성공", data));
    }

    /** 3.2 탈퇴 회원 목록 조회 (페이징) */
    @GetMapping("/withdrawn")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getWithdrawnUsers(
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<UserResponse> data = userService.getWithdrawnUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success("탈퇴 회원 목록 조회 성공", data));
    }

    /** 3.3 회원 상세 조회 (탈퇴 회원 포함) */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserDetail(@PathVariable Long userId) {
        UserResponse data = userService.getUserDetail(userId);
        return ResponseEntity.ok(ApiResponse.success("회원 상세 조회 성공", data));
    }

    /** 3.4 회원 강제 삭제 */
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetailsImpl adminDetails) {
        userService.deleteUser(userId, adminDetails.userId());
        return ResponseEntity.ok(ApiResponse.success("회원 삭제 완료"));
    }

    /** 3.5 회원 역할(권한) 변경 */
    @PatchMapping("/{userId}/role")
    public ResponseEntity<ApiResponse<Void>> changeRole(
            @PathVariable Long userId,
            @Valid @RequestBody ChangeRoleRequest request,
            @AuthenticationPrincipal UserDetailsImpl adminDetails) {

        // "ROLE_" 접두사 길이(5)만큼 substring 하여 안전하게 Enum 코드로 변환
        String roleCode = adminDetails.role().substring(5);
        UserRole executorRole = UserRole.valueOf(roleCode);

        String message = userService.changeRole(userId, request, executorRole);
        return ResponseEntity.ok(ApiResponse.success(message));
    }
}
