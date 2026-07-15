package com.sparta.spartachallenge8282.menu.application;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import com.sparta.spartachallenge8282.menu.domain.Menu;
import com.sparta.spartachallenge8282.menu.domain.MenuBadge;
import com.sparta.spartachallenge8282.menu.domain.MenuRepository;
import com.sparta.spartachallenge8282.menu.domain.MenuStatus;
import com.sparta.spartachallenge8282.menu.presentation.dto.response.MenuCreateResponse;
import com.sparta.spartachallenge8282.menu.presentation.dto.response.MenuDeleteResponse;
import com.sparta.spartachallenge8282.option.domain.MenuOption;
import com.sparta.spartachallenge8282.option.domain.MenuOptionRepository;
import com.sparta.spartachallenge8282.optiongroup.domain.MenuOptionGroup;
import com.sparta.spartachallenge8282.optiongroup.domain.MenuOptionGroupRepository;
import com.sparta.spartachallenge8282.menu.presentation.dto.request.MenuCreateRequest;
import com.sparta.spartachallenge8282.menu.presentation.dto.request.MenuUpdateRequest;
import com.sparta.spartachallenge8282.menu.presentation.dto.response.MenuResponse;
import com.sparta.spartachallenge8282.store.application.OwnerStoreService;
import com.sparta.spartachallenge8282.store.domain.StoreRepository;
import com.sparta.spartachallenge8282.user.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * MenuService 단위 테스트 — Repository 를 {@code @Mock} 으로 대체해 서비스 로직만 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private MenuOptionGroupRepository optionGroupRepository;

    @Mock
    private MenuOptionRepository optionRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private OwnerStoreService ownerStoreService;

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

    private MenuOptionGroup sampleGroup(UUID menuId) {
        return MenuOptionGroup.builder()
                .menuId(menuId)
                .name("음료 선택")
                .isRequired(true)
                .minSelect(1)
                .maxSelect(1)
                .sortOrder(1)
                .isActive(true)
                .build();
    }

    private MenuOption sampleOption(UUID optionGroupId) {
        return MenuOption.builder()
                .optionGroupId(optionGroupId)
                .name("콜라")
                .additionalPrice(1000)
                .sortOrder(1)
                .isActive(true)
                .build();
    }

    private UserDetailsImpl managerUser() {
        return new UserDetailsImpl(1L, "manager@test.com", UserRole.MANAGER.getAuthority());
    }

    private UserDetailsImpl ownerUser(Long userId) {
        return new UserDetailsImpl(userId, "owner@test.com", UserRole.OWNER.getAuthority());
    }

    private UserDetailsImpl customerUser() {
        return new UserDetailsImpl(2L, "customer@test.com", UserRole.CUSTOMER.getAuthority());
    }

    private UserDetailsImpl masterUser() {
        return new UserDetailsImpl(3L, "master@test.com", UserRole.MASTER.getAuthority());
    }

    private void givenStoreExists(UUID storeId) {
        given(storeRepository.existsByIdAndDeletedAtIsNull(storeId)).willReturn(true);
    }

    private void givenOwnerOwnsStore(UUID storeId, Long ownerId) {
        given(storeRepository.existsByIdAndOwner_IdAndDeletedAtIsNull(storeId, ownerId)).willReturn(true);
    }

    // ── 생성 ────────────────────────────────────────────────────────────────

    @Test
    void 메뉴생성_성공하면_생성된_id를_반환한다() {
        // given
        UUID storeId = UUID.randomUUID();
        MenuCreateRequest request = new MenuCreateRequest(
                "후라이드", "바삭한 후라이드", 18000, 1,
                MenuStatus.ON_SALE, MenuBadge.NONE);

        UUID generatedId = UUID.randomUUID();
        Menu saved = sampleMenu(storeId);
        ReflectionTestUtils.setField(saved, "id", generatedId);
        givenStoreExists(storeId);
        given(menuRepository.save(any(Menu.class))).willReturn(saved);

        // when
        MenuCreateResponse result = menuService.createMenu(storeId, request, managerUser());

        // then
        assertThat(result.menuId()).isEqualTo(generatedId);
    }

    @Test
    void 메뉴생성_가격이_음수면_INVALID_MENU_PRICE를_던진다() {
        // given
        UUID storeId = UUID.randomUUID();
        MenuCreateRequest request = new MenuCreateRequest(
                "후라이드", "바삭한 후라이드", -1000, 1,
                null, null);
        givenStoreExists(storeId);

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> menuService.createMenu(storeId, request, managerUser()));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_MENU_PRICE);
        verify(menuRepository, never()).save(any());
    }

    @Test
    void 메뉴생성_isAiGenerated는_항상_false로_저장된다() {
        // given
        UUID storeId = UUID.randomUUID();
        MenuCreateRequest request = new MenuCreateRequest(
                "후라이드", "바삭한 후라이드", 18000, 1,
                MenuStatus.ON_SALE, MenuBadge.NONE);

        givenStoreExists(storeId);
        given(menuRepository.save(any(Menu.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        menuService.createMenu(storeId, request, managerUser());

        // then
        ArgumentCaptor<Menu> captor = ArgumentCaptor.forClass(Menu.class);
        verify(menuRepository).save(captor.capture());
        assertThat(captor.getValue().isAiGenerated()).isFalse();
    }

    @Test
    void 메뉴생성_없는가게면_STORE_NOT_FOUND를_던진다() {
        // given
        UUID storeId = UUID.randomUUID();
        MenuCreateRequest request = new MenuCreateRequest(
                "후라이드", "바삭한 후라이드", 18000, 1,
                MenuStatus.ON_SALE, MenuBadge.NONE);
        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> menuService.createMenu(storeId, request, managerUser()));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.STORE_NOT_FOUND);
        verify(menuRepository, never()).save(any());
    }

    @Test
    void 메뉴생성_OWNER가_본인가게가_아니면_NO_MENU_PERMISSION을_던진다() {
        // given
        UUID storeId = UUID.randomUUID();
        UserDetailsImpl owner = ownerUser(1L);
        MenuCreateRequest request = new MenuCreateRequest(
                "후라이드", "바삭한 후라이드", 18000, 1,
                MenuStatus.ON_SALE, MenuBadge.NONE);
        givenStoreExists(storeId);

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> menuService.createMenu(storeId, request, owner));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NO_MENU_PERMISSION);
        verify(menuRepository, never()).save(any());
    }

    @Test
    void 메뉴생성_OWNER가_본인가게면_성공한다() {
        // given
        UUID storeId = UUID.randomUUID();
        UserDetailsImpl owner = ownerUser(1L);
        MenuCreateRequest request = new MenuCreateRequest(
                "후라이드", "바삭한 후라이드", 18000, 1,
                MenuStatus.ON_SALE, MenuBadge.NONE);
        Menu saved = sampleMenu(storeId);
        ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
        givenStoreExists(storeId);
        givenOwnerOwnsStore(storeId, owner.userId());
        given(menuRepository.save(any(Menu.class))).willReturn(saved);

        // when
        MenuCreateResponse result = menuService.createMenu(storeId, request, owner);

        // then
        assertThat(result.menuId()).isEqualTo(saved.getId());
    }

    @Test
    void 메뉴생성_CUSTOMER면_ACCESS_DENIED를_던진다() {
        // given
        UUID storeId = UUID.randomUUID();
        MenuCreateRequest request = new MenuCreateRequest(
                "후라이드", "바삭한 후라이드", 18000, 1,
                MenuStatus.ON_SALE, MenuBadge.NONE);

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> menuService.createMenu(storeId, request, customerUser()));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
        verify(storeRepository, never()).existsByIdAndDeletedAtIsNull(any());
        verify(menuRepository, never()).save(any());
    }

    @Test
    void 메뉴생성_MASTER면_성공한다() {
        // given
        UUID storeId = UUID.randomUUID();
        MenuCreateRequest request = new MenuCreateRequest(
                "후라이드", "바삭한 후라이드", 18000, 1,
                MenuStatus.ON_SALE, MenuBadge.NONE);
        Menu saved = sampleMenu(storeId);
        ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
        givenStoreExists(storeId);
        given(menuRepository.save(any(Menu.class))).willReturn(saved);

        // when
        MenuCreateResponse result = menuService.createMenu(storeId, request, masterUser());

        // then
        assertThat(result.menuId()).isEqualTo(saved.getId());
        verify(storeRepository, never())
                .existsByIdAndOwner_IdAndDeletedAtIsNull(any(), any());
    }

    // ── 단건 조회 ──────────────────────────────────────────────────────────────

    @Test
    void 단건조회_성공하면_MenuResponse를_반환한다() {
        // given
        UUID id = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Menu menu = sampleMenu(storeId);
        ReflectionTestUtils.setField(menu, "id", id);
        given(menuRepository.findByIdAndDeletedAtIsNullAndIsHiddenFalse(id)).willReturn(Optional.of(menu));

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
        given(menuRepository.findByIdAndDeletedAtIsNullAndIsHiddenFalse(id)).willReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> menuService.getMenu(id));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MENU_NOT_FOUND);
    }

    @Test
    void 단건조회_숨김메뉴는_목록과_동일하게_MENU_NOT_FOUND를_던진다() {
        // given
        UUID id = UUID.randomUUID();
        given(menuRepository.findByIdAndDeletedAtIsNullAndIsHiddenFalse(id)).willReturn(Optional.empty());

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
        givenStoreExists(storeId);

        // when
        MenuResponse result = menuService.updateMenu(id, request, managerUser());

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
                () -> menuService.updateMenu(id, request, managerUser()));

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
        givenStoreExists(storeId);

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> menuService.updateMenu(id, request, managerUser()));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_MENU_PRICE);
    }

    @Test
    void 메뉴수정_다른_description이면_교체하고_isAiGenerated를_false로_내린다() {
        // given
        UUID id = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Menu menu = sampleMenu(storeId);
        ReflectionTestUtils.setField(menu, "id", id);
        menu.applyAiDescription("AI 설명");
        MenuUpdateRequest request = new MenuUpdateRequest(
                null, "직접 수정한 설명", null, null, null, null);

        given(menuRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.of(menu));
        givenStoreExists(storeId);

        // when
        MenuResponse result = menuService.updateMenu(id, request, managerUser());

        // then
        assertThat(result.description()).isEqualTo("직접 수정한 설명");
        assertThat(result.isAiGenerated()).isFalse();
    }

    @Test
    void 메뉴수정_동일_description이면_isAiGenerated를_유지한다() {
        // given
        UUID id = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Menu menu = sampleMenu(storeId);
        ReflectionTestUtils.setField(menu, "id", id);
        menu.applyAiDescription("AI 설명");
        MenuUpdateRequest request = new MenuUpdateRequest(
                null, "AI 설명", null, null, null, null);

        given(menuRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.of(menu));
        givenStoreExists(storeId);

        // when
        MenuResponse result = menuService.updateMenu(id, request, managerUser());

        // then
        assertThat(result.description()).isEqualTo("AI 설명");
        assertThat(result.isAiGenerated()).isTrue();
    }

    @Test
    void 메뉴수정_description이_null이면_기존_설명과_isAiGenerated를_유지한다() {
        // given
        UUID id = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Menu menu = sampleMenu(storeId);
        ReflectionTestUtils.setField(menu, "id", id);
        menu.applyAiDescription("AI 설명");
        MenuUpdateRequest request = new MenuUpdateRequest(
                null, null, 19000, null, null, null);

        given(menuRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.of(menu));
        givenStoreExists(storeId);

        // when
        MenuResponse result = menuService.updateMenu(id, request, managerUser());

        // then
        assertThat(result.description()).isEqualTo("AI 설명");
        assertThat(result.price()).isEqualTo(19000);
        assertThat(result.isAiGenerated()).isTrue();
    }

    // ── AI 설명 반영 ──────────────────────────────────────────────────────────

    @Test
    void AI메뉴설명반영_성공하면_description을_수정하고_isAiGenerated를_true로_변경한다() {
        // given
        UUID id = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Menu menu = sampleMenu(storeId);
        ReflectionTestUtils.setField(menu, "id", id);

        given(menuRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.of(menu));
        givenStoreExists(storeId);

        // when
        MenuResponse result = menuService.applyAiDescription(id, "AI가 생성한 바삭한 후라이드 설명", managerUser());

        // then
        assertThat(result.description()).isEqualTo("AI가 생성한 바삭한 후라이드 설명");
        assertThat(result.isAiGenerated()).isTrue();
    }

    @Test
    void AI메뉴설명반영_OWNER가_본인가게가_아니면_NO_MENU_PERMISSION을_던진다() {
        // given
        UUID menuId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UserDetailsImpl owner = ownerUser(1L);
        Menu menu = sampleMenu(storeId);
        ReflectionTestUtils.setField(menu, "id", menuId);

        given(menuRepository.findByIdAndDeletedAtIsNull(menuId)).willReturn(Optional.of(menu));
        givenStoreExists(storeId);
        given(storeRepository.existsByIdAndOwner_IdAndDeletedAtIsNull(storeId, owner.userId()))
                .willReturn(false);

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> menuService.applyAiDescription(menuId, "AI 설명", owner));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NO_MENU_PERMISSION);
        assertThat(menu.getDescription()).isEqualTo("바삭한 후라이드");
        assertThat(menu.isAiGenerated()).isFalse();
        verify(storeRepository)
                .existsByIdAndOwner_IdAndDeletedAtIsNull(storeId, owner.userId());
    }

    @Test
    void AI메뉴설명반영_없는id는_MENU_NOT_FOUND를_던진다() {
        // given
        UUID id = UUID.randomUUID();
        given(menuRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> menuService.applyAiDescription(id, "AI 설명", managerUser()));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MENU_NOT_FOUND);
    }

    // ── 삭제 ────────────────────────────────────────────────────────────────

    @Test
    void 메뉴삭제_성공하면_deletedAt을_반환하고_소프트삭제된다() {
        // given
        UUID id = UUID.randomUUID();
        Long userId = 1L;
        UserDetailsImpl user = managerUser();
        UUID storeId = UUID.randomUUID();
        Menu menu = sampleMenu(storeId);
        ReflectionTestUtils.setField(menu, "id", id);

        given(menuRepository.findById(id)).willReturn(Optional.of(menu));
        givenStoreExists(storeId);
        given(optionGroupRepository.findAllByMenuIdAndDeletedAtIsNull(id)).willReturn(List.of());

        // when
        MenuDeleteResponse result = menuService.deleteMenu(id, user);

        // then
        assertThat(result.deletedAt()).isNotNull();
        assertThat(menu.isDeleted()).isTrue();
        assertThat(menu.getDeletedAt()).isEqualTo(result.deletedAt());
        assertThat(result.menuId()).isEqualTo(id);
        assertThat(menu.getDeletedBy()).isEqualTo(userId);
        verify(ownerStoreService).refreshOperationStatusByMenus(storeId);
    }

    @Test
    void 메뉴삭제_성공하면_하위옵션그룹과_옵션도_소프트삭제된다() {
        // given
        UUID id = UUID.randomUUID();
        Long userId = 1L;
        UserDetailsImpl user = managerUser();
        UUID storeId = UUID.randomUUID();
        Menu menu = sampleMenu(storeId);
        MenuOptionGroup group = sampleGroup(id);
        MenuOption option = sampleOption(UUID.randomUUID());
        ReflectionTestUtils.setField(menu, "id", id);
        ReflectionTestUtils.setField(group, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(option, "id", UUID.randomUUID());

        given(menuRepository.findById(id)).willReturn(Optional.of(menu));
        givenStoreExists(storeId);
        given(optionGroupRepository.findAllByMenuIdAndDeletedAtIsNull(id)).willReturn(List.of(group));
        given(optionRepository.findAllByOptionGroupIdInAndDeletedAtIsNull(List.of(group.getId()))).willReturn(List.of(option));

        // when
        menuService.deleteMenu(id, user);

        // then
        assertThat(menu.isDeleted()).isTrue();
        assertThat(group.isDeleted()).isTrue();
        assertThat(option.isDeleted()).isTrue();
        assertThat(group.getDeletedBy()).isEqualTo(userId);
        assertThat(option.getDeletedBy()).isEqualTo(userId);
        verify(ownerStoreService).refreshOperationStatusByMenus(storeId);
    }

    @Test
    void 메뉴삭제_없는id는_MENU_NOT_FOUND를_던진다() {
        // given
        UUID id = UUID.randomUUID();
        given(menuRepository.findById(id)).willReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> menuService.deleteMenu(id, managerUser()));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MENU_NOT_FOUND);
        verifyNoInteractions(ownerStoreService);
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
                () -> menuService.deleteMenu(id, managerUser()));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ALREADY_DELETED_MENU);
        verifyNoInteractions(ownerStoreService);
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

    @Test
    void 목록조회_size가_허용값이_아니면_10으로_정규화해_검색한다() {
        // given
        UUID storeId = UUID.randomUUID();
        Pageable requested = PageRequest.of(2, 25, Sort.by(Sort.Direction.DESC, "createdAt"));
        Pageable normalized = PageRequest.of(2, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Menu> page = new PageImpl<>(List.of(), normalized, 0);

        given(menuRepository.searchMenus(storeId, "", null, null, false, normalized)).willReturn(page);

        // when
        menuService.getMenuList(storeId, null, null, null, requested);

        // then — page/sort 는 유지하고 size 만 10으로 보정한다
        verify(menuRepository).searchMenus(storeId, "", null, null, false, normalized);
    }

    @Test
    void 메뉴수정_OWNER가_본인가게가_아니면_NO_MENU_PERMISSION을_던진다() {
        // given
        UUID menuId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Menu menu = sampleMenu(storeId);
        ReflectionTestUtils.setField(menu, "id", menuId);
        MenuUpdateRequest request = new MenuUpdateRequest(
                "양념치킨", null, null, null, null, null);
        given(menuRepository.findByIdAndDeletedAtIsNull(menuId)).willReturn(Optional.of(menu));
        givenStoreExists(storeId);

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> menuService.updateMenu(menuId, request, ownerUser(1L)));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NO_MENU_PERMISSION);
        assertThat(menu.getName()).isEqualTo("후라이드");
    }

    @Test
    void 메뉴삭제_OWNER가_본인가게가_아니면_NO_MENU_PERMISSION을_던진다() {
        // given
        UUID menuId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Menu menu = sampleMenu(storeId);
        ReflectionTestUtils.setField(menu, "id", menuId);
        given(menuRepository.findById(menuId)).willReturn(Optional.of(menu));
        givenStoreExists(storeId);

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> menuService.deleteMenu(menuId, ownerUser(1L)));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NO_MENU_PERMISSION);
        assertThat(menu.isDeleted()).isFalse();
        verifyNoInteractions(optionGroupRepository, optionRepository);
        verifyNoInteractions(ownerStoreService);
    }
}