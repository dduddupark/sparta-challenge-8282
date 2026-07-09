package com.sparta.spartachallenge8282.menu.optiongroup.presentation;

import com.sparta.spartachallenge8282.global.common.ApiResponse;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import com.sparta.spartachallenge8282.menu.optiongroup.application.MenuOptionGroupService;
import com.sparta.spartachallenge8282.menu.optiongroup.presentation.dto.request.MenuOptionGroupCreateRequest;
import com.sparta.spartachallenge8282.menu.optiongroup.presentation.dto.request.MenuOptionGroupUpdateRequest;
import com.sparta.spartachallenge8282.menu.optiongroup.presentation.dto.response.MenuOptionGroupCreateResponse;
import com.sparta.spartachallenge8282.menu.optiongroup.presentation.dto.response.MenuOptionGroupDeleteResponse;
import com.sparta.spartachallenge8282.menu.optiongroup.presentation.dto.response.MenuOptionGroupResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 옵션 그룹 REST 컨트롤러.
 *
 * <p>경로가 갈려 클래스 {@code @RequestMapping} 없이 메서드마다 전체 경로 명시.
 * 생성/목록은 메뉴 하위({@code /menus/{menuId}/option-groups}), 단건/수정/삭제는 {@code /option-groups/{optionGroupId}}.
 * 쓰기는 OWNER/MANAGER/MASTER(소유권 검증은 auth 브랜치), 조회는 비로그인 공개.
 */
@RestController
@RequiredArgsConstructor
public class MenuOptionGroupController {

    private final MenuOptionGroupService optionGroupService;

    @PreAuthorize("hasAnyAuthority('ROLE_OWNER','ROLE_MANAGER','ROLE_MASTER')")
    @PostMapping("/api/v1/menus/{menuId}/option-groups")
    public ResponseEntity<ApiResponse<MenuOptionGroupCreateResponse>> createOptionGroup(
            @PathVariable UUID menuId,
            @Valid @RequestBody MenuOptionGroupCreateRequest request) {
        UUID id = optionGroupService.createOptionGroup(menuId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("옵션 그룹 생성 완료", new MenuOptionGroupCreateResponse(id)));
    }

    @GetMapping("/api/v1/menus/{menuId}/option-groups")
    public ResponseEntity<ApiResponse<List<MenuOptionGroupResponse>>> getOptionGroupList(
            @PathVariable UUID menuId) {
        return ResponseEntity.ok(
                ApiResponse.success("옵션 그룹 목록 조회 성공", optionGroupService.getOptionGroupList(menuId)));
    }

    @GetMapping("/api/v1/option-groups/{optionGroupId}")
    public ResponseEntity<ApiResponse<MenuOptionGroupResponse>> getOptionGroup(
            @PathVariable UUID optionGroupId) {
        return ResponseEntity.ok(
                ApiResponse.success("옵션 그룹 조회 성공", optionGroupService.getOptionGroup(optionGroupId)));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_OWNER','ROLE_MANAGER','ROLE_MASTER')")
    @PatchMapping("/api/v1/option-groups/{optionGroupId}")
    public ResponseEntity<ApiResponse<MenuOptionGroupResponse>> updateOptionGroup(
            @PathVariable UUID optionGroupId,
            @Valid @RequestBody MenuOptionGroupUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("옵션 그룹 수정 완료", optionGroupService.updateOptionGroup(optionGroupId, request)));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_OWNER','ROLE_MANAGER','ROLE_MASTER')")
    @DeleteMapping("/api/v1/option-groups/{optionGroupId}")
    public ResponseEntity<ApiResponse<MenuOptionGroupDeleteResponse>> deleteOptionGroup(
            @PathVariable UUID optionGroupId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        LocalDateTime deletedAt = optionGroupService.deleteOptionGroup(optionGroupId, userDetails.userId());
        return ResponseEntity.ok(
                ApiResponse.success("옵션 그룹 삭제 완료", new MenuOptionGroupDeleteResponse(optionGroupId, deletedAt)));
    }
}
