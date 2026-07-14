package com.sparta.spartachallenge8282.menu.presentation;

import com.sparta.spartachallenge8282.global.common.ApiResponse;
import com.sparta.spartachallenge8282.global.common.PageResponse;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import com.sparta.spartachallenge8282.menu.application.MenuService;
import com.sparta.spartachallenge8282.menu.domain.MenuBadge;
import com.sparta.spartachallenge8282.menu.domain.MenuStatus;
import com.sparta.spartachallenge8282.menu.presentation.dto.request.MenuAiDescriptionUpdateRequest;
import com.sparta.spartachallenge8282.menu.presentation.dto.request.MenuCreateRequest;
import com.sparta.spartachallenge8282.menu.presentation.dto.request.MenuUpdateRequest;
import com.sparta.spartachallenge8282.menu.presentation.dto.response.MenuCreateResponse;
import com.sparta.spartachallenge8282.menu.presentation.dto.response.MenuDeleteResponse;
import com.sparta.spartachallenge8282.menu.presentation.dto.response.MenuResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 메뉴 REST 컨트롤러.
 *
 * <p>메뉴 생성/목록은 가게 하위 경로를 사용하고,
 * 단건 조회/수정/삭제/AI 설명 적용은 메뉴 ID 기준 경로를 사용한다.
 *
 * <p>쓰기 요청은 OWNER/MANAGER/MASTER 권한이 필요하며,
 * 조회는 비로그인 공개다.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @PreAuthorize("hasAnyAuthority('ROLE_OWNER','ROLE_MANAGER','ROLE_MASTER')")
    @PostMapping("/stores/{storeId}/menus")
    public ResponseEntity<ApiResponse<MenuCreateResponse>> createMenu(
            @PathVariable UUID storeId,
            @Valid @RequestBody MenuCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("메뉴 생성 완료", menuService.createMenu(storeId, request)));
    }

    @GetMapping("/menus/{menuId}")
    public ResponseEntity<ApiResponse<MenuResponse>> getMenu(@PathVariable UUID menuId) {
        return ResponseEntity.ok(
                ApiResponse.success("메뉴 조회 성공", menuService.getMenu(menuId)));
    }

    @GetMapping("/stores/{storeId}/menus")
    public ResponseEntity<ApiResponse<PageResponse<MenuResponse>>> getMenuList(
            @PathVariable UUID storeId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) MenuStatus status,
            @RequestParam(required = false) MenuBadge badge,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<MenuResponse> data =
                PageResponse.from(menuService.getMenuList(storeId, keyword, status, badge, pageable));
        return ResponseEntity.ok(ApiResponse.success("메뉴 목록 조회 성공", data));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_OWNER','ROLE_MANAGER','ROLE_MASTER')")
    @PatchMapping("/menus/{menuId}")
    public ResponseEntity<ApiResponse<MenuResponse>> updateMenu(
            @PathVariable UUID menuId,
            @Valid @RequestBody MenuUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("메뉴 수정 완료", menuService.updateMenu(menuId, request)));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_OWNER','ROLE_MANAGER','ROLE_MASTER')")
    @PatchMapping("/menus/{menuId}/ai-description")
    public ResponseEntity<ApiResponse<MenuResponse>> updateAiDescription(
            @PathVariable UUID menuId,
            @Valid @RequestBody MenuAiDescriptionUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("AI 메뉴 설명 적용 완료", menuService.applyAiDescription(menuId, request.description())));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_OWNER','ROLE_MANAGER','ROLE_MASTER')")
    @DeleteMapping("/menus/{menuId}")
    public ResponseEntity<ApiResponse<MenuDeleteResponse>> deleteMenu(
            @PathVariable UUID menuId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(
                ApiResponse.success("메뉴 삭제 완료", menuService.deleteMenu(menuId, userDetails.userId())));
    }
}
