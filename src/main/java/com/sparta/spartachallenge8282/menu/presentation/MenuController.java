package com.sparta.spartachallenge8282.menu.presentation;

import com.sparta.spartachallenge8282.global.common.ApiResponse;
import com.sparta.spartachallenge8282.global.common.PageResponse;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import com.sparta.spartachallenge8282.menu.application.MenuService;
import com.sparta.spartachallenge8282.menu.domain.MenuBadge;
import com.sparta.spartachallenge8282.menu.domain.MenuStatus;
import com.sparta.spartachallenge8282.menu.presentation.dto.request.MenuCreateRequest;
import com.sparta.spartachallenge8282.menu.presentation.dto.request.MenuUpdateRequest;
import com.sparta.spartachallenge8282.menu.presentation.dto.response.MenuCreateResponse;
import com.sparta.spartachallenge8282.menu.presentation.dto.response.MenuDeleteResponse;
import com.sparta.spartachallenge8282.menu.presentation.dto.response.MenuResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 메뉴 REST 컨트롤러.
 *
 * <p>경로가 두 갈래라 클래스 {@code @RequestMapping} 을 두지 않고 메서드마다 전체 경로를 명시한다.
 * <ul>
 *   <li>생성/목록: {@code /api/v1/stores/{storeId}/menus} — 메뉴는 가게에 종속되므로 가게 하위 경로. 목록은 숨김 제외 + 페이징.</li>
 *   <li>단건/수정/삭제: {@code /api/v1/menus/{menuId}}</li>
 * </ul>
 *
 * <p><b>권한:</b> 쓰기는 OWNER/MANAGER/MASTER. 단, OWNER 의 "본인 가게" 소유권 검증은
 * {@code @PreAuthorize} 의 role 체크만으론 부족해 Service 에서 storeId 로 확인해야 한다
 * (NO_MENU_PERMISSION — store 연동 auth 브랜치). 조회는 비로그인 공개(SecurityConfig).
 */
@RestController
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @PreAuthorize("hasAnyAuthority('ROLE_OWNER','ROLE_MANAGER','ROLE_MASTER')")
    @PostMapping("/api/v1/stores/{storeId}/menus")
    public ResponseEntity<ApiResponse<MenuCreateResponse>> createMenu(
            @PathVariable UUID storeId,
            @Valid @RequestBody MenuCreateRequest request) {
        UUID menuId = menuService.createMenu(storeId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("메뉴 생성 완료", new MenuCreateResponse(menuId)));
    }

    @GetMapping("/api/v1/menus/{menuId}")
    public ResponseEntity<ApiResponse<MenuResponse>> getMenu(@PathVariable UUID menuId) {
        return ResponseEntity.ok(
                ApiResponse.success("메뉴 조회 성공", menuService.getMenu(menuId)));
    }

    @GetMapping("/api/v1/stores/{storeId}/menus")
    public ResponseEntity<ApiResponse<PageResponse<MenuResponse>>> getMenuList(
            @PathVariable UUID storeId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) MenuStatus status,
            @RequestParam(required = false) MenuBadge badge,
            @PageableDefault(size = 10, sort = "sortOrder") Pageable pageable) {
        PageResponse<MenuResponse> data =
                PageResponse.from(menuService.getMenuList(storeId, keyword, status, badge, pageable));
        return ResponseEntity.ok(ApiResponse.success("메뉴 목록 조회 성공", data));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_OWNER','ROLE_MANAGER','ROLE_MASTER')")
    @PatchMapping("/api/v1/menus/{menuId}")
    public ResponseEntity<ApiResponse<MenuResponse>> updateMenu(
            @PathVariable UUID menuId,
            @Valid @RequestBody MenuUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("메뉴 수정 완료", menuService.updateMenu(menuId, request)));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_OWNER','ROLE_MANAGER','ROLE_MASTER')")
    @DeleteMapping("/api/v1/menus/{menuId}")
    public ResponseEntity<ApiResponse<MenuDeleteResponse>> deleteMenu(
            @PathVariable UUID menuId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        LocalDateTime deletedAt = menuService.deleteMenu(menuId, userDetails.userId());
        return ResponseEntity.ok(
                ApiResponse.success("메뉴 삭제 완료", new MenuDeleteResponse(menuId, deletedAt)));
    }
}
