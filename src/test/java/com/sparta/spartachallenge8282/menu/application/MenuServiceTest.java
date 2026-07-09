package com.sparta.spartachallenge8282.menu.application;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.menu.domain.Menu;
import com.sparta.spartachallenge8282.menu.domain.MenuBadge;
import com.sparta.spartachallenge8282.menu.domain.MenuRepository;
import com.sparta.spartachallenge8282.menu.domain.MenuStatus;
import com.sparta.spartachallenge8282.menu.presentation.dto.request.MenuCreateRequest;
import com.sparta.spartachallenge8282.menu.presentation.dto.request.MenuUpdateRequest;
import com.sparta.spartachallenge8282.menu.presentation.dto.response.MenuResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * MenuService 단위 테스트 — Repository 를 {@code @Mock} 으로 대체해 서비스 로직만 검증한다.
 * store 연동(STORE_NOT_FOUND)·권한(NO_MENU_PERMISSION) 케이스는 후속 브랜치에서 추가한다.
 */
@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Mock
    private MenuRepository menuRepository;

    @InjectMocks
    private MenuService menuService;

    private Menu sampleMenu(UUID storeId) {
        return Menu.builder()
                .storeId(storeId)
                .name("후라이드")
                .description("바삭한 후라이드")
                .price(18000)
                .sortOrder(1)
                .status(MenuStatus.ON_SALE)
                .badge(MenuBadge.NONE)
                .build();
    }

    // ── 생성 ────────────────────────────────────────────────────────────────

    @Test
    void 메뉴생성_성공하면_생성된_id를_반환한다() {
        // given
        UUID storeId = UUID.randomUUID();
        MenuCreateRequest request = new MenuCreateRequest(
                "후라이드", "바삭한 후라이드", 18000, 1,
                MenuStatus.ON_SALE, MenuBadge.NONE, false);

        UUID generatedId = UUID.randomUUID();
        Menu saved = sampleMenu(storeId);
        ReflectionTestUtils.setField(saved, "id", generatedId);
        given(menuRepository.save(any(Menu.class))).willReturn(saved);

        // when
        UUID result = menuService.createMenu(storeId, request);

        // then
        assertThat(result).isEqualTo(generatedId);
    }

    @Test
    void 메뉴생성_가격이_음수면_INVALID_MENU_PRICE를_던진다() {
        // given
        UUID storeId = UUID.randomUUID();
        MenuCreateRequest request = new MenuCreateRequest(
                "후라이드", "바삭한 후라이드", -1000, 1,
                null, null, false);

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> menuService.createMenu(storeId, request));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_MENU_PRICE);
        verify(menuRepository, never()).save(any());
    }

    // ── 단건 조회 ──────────────────────────────────────────────────────────────

    @Test
    void 단건조회_성공하면_MenuResponse를_반환한다() {
        // given
        UUID id = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Menu menu = sampleMenu(storeId);
        ReflectionTestUtils.setField(menu, "id", id);
        given(menuRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.of(menu));

        // when
        MenuResponse result = menuService.getMenu(id);

        // then
        assertThat(result.menuId()).isEqualTo(id);
        assertThat(result.storeId()).isEqualTo(storeId);
        assertThat(result.name()).isEqualTo("후라이드");
        assertThat(result.price()).isEqualTo(18000);
        assertThat(result.status()).isEqualTo(MenuStatus.ON_SALE);
    }

    @Test
    void 단건조회_없는id는_MENU_NOT_FOUND를_던진다() {
        // given
        UUID id = UUID.randomUUID();
        given(menuRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> menuService.getMenu(id));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MENU_NOT_FOUND);
    }

    // ── 수정 ────────────────────────────────────────────────────────────────

    @Test
    void 메뉴수정_성공하면_수정된_MenuResponse를_반환한다() {
        // given
        UUID id = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Menu menu = sampleMenu(storeId);
        ReflectionTestUtils.setField(menu, "id", id);
        MenuUpdateRequest request = new MenuUpdateRequest(
                "양념치킨", "매콤달콤", 19000, 2, MenuStatus.SOLD_OUT, MenuBadge.DISCOUNT);

        given(menuRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.of(menu));

        // when
        MenuResponse result = menuService.updateMenu(id, request);

        // then
        assertThat(result.name()).isEqualTo("양념치킨");
        assertThat(result.price()).isEqualTo(19000);
        assertThat(result.sortOrder()).isEqualTo(2);
        assertThat(result.status()).isEqualTo(MenuStatus.SOLD_OUT);
        assertThat(result.badge()).isEqualTo(MenuBadge.DISCOUNT);
    }

    @Test
    void 메뉴수정_없는id는_MENU_NOT_FOUND를_던진다() {
        // given
        UUID id = UUID.randomUUID();
        MenuUpdateRequest request = new MenuUpdateRequest(
                "양념치킨", null, null, null, null, null);
        given(menuRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> menuService.updateMenu(id, request));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MENU_NOT_FOUND);
    }

    @Test
    void 메뉴수정_가격이_음수면_INVALID_MENU_PRICE를_던진다() {
        // given
        UUID id = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Menu menu = sampleMenu(storeId);
        ReflectionTestUtils.setField(menu, "id", id);
        MenuUpdateRequest request = new MenuUpdateRequest(
                null, null, -500, null, null, null);

        given(menuRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.of(menu));

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> menuService.updateMenu(id, request));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_MENU_PRICE);
    }

    // ── 삭제 ────────────────────────────────────────────────────────────────

    @Test
    void 메뉴삭제_성공하면_deletedAt을_반환하고_소프트삭제된다() {
        // given
        UUID id = UUID.randomUUID();
        Long userId = 1L;
        Menu menu = sampleMenu(UUID.randomUUID());
        ReflectionTestUtils.setField(menu, "id", id);

        given(menuRepository.findById(id)).willReturn(Optional.of(menu));

        // when
        LocalDateTime deletedAt = menuService.deleteMenu(id, userId);

        // then
        assertThat(deletedAt).isNotNull();
        assertThat(menu.isDeleted()).isTrue();
        assertThat(menu.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(menu.getDeletedBy()).isEqualTo(userId);
    }

    @Test
    void 메뉴삭제_없는id는_MENU_NOT_FOUND를_던진다() {
        // given
        UUID id = UUID.randomUUID();
        given(menuRepository.findById(id)).willReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> menuService.deleteMenu(id, 1L));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MENU_NOT_FOUND);
    }

    @Test
    void 메뉴삭제_이미삭제된_메뉴면_ALREADY_DELETED_MENU를_던진다() {
        // given
        UUID id = UUID.randomUUID();
        Menu menu = sampleMenu(UUID.randomUUID());
        ReflectionTestUtils.setField(menu, "id", id);
        ReflectionTestUtils.setField(menu, "deletedAt", LocalDateTime.now());

        given(menuRepository.findById(id)).willReturn(Optional.of(menu));

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> menuService.deleteMenu(id, 1L));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ALREADY_DELETED_MENU);
    }

    // ── 목록 조회 ──────────────────────────────────────────────────────────────

    @Test
    void 목록조회_성공하면_페이징된_MenuResponse를_반환한다() {
        // given
        UUID storeId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        UUID id = UUID.randomUUID();
        Menu menu = sampleMenu(storeId);
        ReflectionTestUtils.setField(menu, "id", id);
        Page<Menu> page = new PageImpl<>(List.of(menu), pageable, 1);

        given(menuRepository.searchMenus(storeId, "후라이드", null, null, false, pageable)).willReturn(page);

        // when
        Page<MenuResponse> result = menuService.getMenuList(storeId, "후라이드", null, null, pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).menuId()).isEqualTo(id);
        assertThat(result.getContent().get(0).name()).isEqualTo("후라이드");
    }

    @Test
    void 목록조회_keyword가_null이면_빈문자열로_검색한다() {
        // given
        UUID storeId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Menu> page = new PageImpl<>(List.of(), pageable, 0);

        given(menuRepository.searchMenus(storeId, "", null, null, false, pageable)).willReturn(page);

        // when
        menuService.getMenuList(storeId, null, null, null, pageable);

        // then — 공개 목록은 숨김 제외(includeHidden=false), keyword 는 null→"" 로 전달
        verify(menuRepository).searchMenus(storeId, "", null, null, false, pageable);
    }
}
