package com.sparta.spartachallenge8282.optiongroup.application;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import com.sparta.spartachallenge8282.menu.domain.Menu;
import com.sparta.spartachallenge8282.menu.domain.MenuBadge;
import com.sparta.spartachallenge8282.menu.domain.MenuRepository;
import com.sparta.spartachallenge8282.menu.domain.MenuStatus;
import com.sparta.spartachallenge8282.option.domain.MenuOption;
import com.sparta.spartachallenge8282.option.domain.MenuOptionRepository;
import com.sparta.spartachallenge8282.optiongroup.domain.MenuOptionGroup;
import com.sparta.spartachallenge8282.optiongroup.domain.MenuOptionGroupRepository;
import com.sparta.spartachallenge8282.optiongroup.presentation.dto.request.MenuOptionGroupCreateRequest;
import com.sparta.spartachallenge8282.optiongroup.presentation.dto.request.MenuOptionGroupUpdateRequest;
import com.sparta.spartachallenge8282.optiongroup.presentation.dto.response.MenuOptionGroupCreateResponse;
import com.sparta.spartachallenge8282.optiongroup.presentation.dto.response.MenuOptionGroupDeleteResponse;
import com.sparta.spartachallenge8282.optiongroup.presentation.dto.response.MenuOptionGroupResponse;
import com.sparta.spartachallenge8282.store.domain.StoreRepository;
import com.sparta.spartachallenge8282.user.domain.UserRole;
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
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MenuOptionGroupServiceTest {

    @Mock
    private MenuOptionGroupRepository optionGroupRepository;

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private MenuOptionRepository optionRepository;

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private MenuOptionGroupService optionGroupService;

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

    @Test
    void 옵션그룹생성_성공하면_생성된_id를_반환한다() {
        // given
        UUID menuId = UUID.randomUUID();
        MenuOptionGroupCreateRequest request =
                new MenuOptionGroupCreateRequest("음료 선택", true, 1, 1, 1, true);

        UUID generatedId = UUID.randomUUID();
        MenuOptionGroup saved = sampleGroup(menuId);
        ReflectionTestUtils.setField(saved, "id", generatedId);
        UUID storeId = UUID.randomUUID();
        Menu menu = sampleMenu(storeId);

        given(menuRepository.findByIdAndDeletedAtIsNull(menuId))
                .willReturn(Optional.of(menu));
        givenStoreExists(storeId);
        given(optionGroupRepository.save(any(MenuOptionGroup.class))).willReturn(saved);

        // when
        MenuOptionGroupCreateResponse result = optionGroupService.createOptionGroup(menuId, request, managerUser());

        // then
        assertThat(result.optionGroupId()).isEqualTo(generatedId);
    }

    @Test
    void 옵션그룹생성_존재하지_않는_메뉴면_MENU_NOT_FOUND를_던진다() {
        // given
        UUID menuId = UUID.randomUUID();
        MenuOptionGroupCreateRequest request =
                new MenuOptionGroupCreateRequest("음료 선택", true, 1, 1, 1, true);
        given(menuRepository.findByIdAndDeletedAtIsNull(menuId)).willReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> optionGroupService.createOptionGroup(menuId, request, managerUser()));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MENU_NOT_FOUND);
        verify(optionGroupRepository, never()).save(any());
    }

    @Test
    void 옵션그룹생성_minSelect가_maxSelect보다_크면_INVALID_OPTION_SELECT_RANGE를_던진다() {
        // given
        UUID menuId = UUID.randomUUID();
        MenuOptionGroupCreateRequest request =
                new MenuOptionGroupCreateRequest("음료 선택", true, 2, 1, 1, true);
        UUID storeId = UUID.randomUUID();
        given(menuRepository.findByIdAndDeletedAtIsNull(menuId))
                .willReturn(Optional.of(sampleMenu(storeId)));
        givenStoreExists(storeId);

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> optionGroupService.createOptionGroup(menuId, request, managerUser()));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_OPTION_SELECT_RANGE);
        verify(optionGroupRepository, never()).save(any());
    }

    @Test
    void 옵션그룹생성_필수그룹인데_minSelect가_0이면_INVALID_OPTION_SELECT_RANGE를_던진다() {
        // given
        UUID menuId = UUID.randomUUID();
        MenuOptionGroupCreateRequest request =
                new MenuOptionGroupCreateRequest("음료 선택", true, 0, 1, 1, true);
        UUID storeId = UUID.randomUUID();
        given(menuRepository.findByIdAndDeletedAtIsNull(menuId))
                .willReturn(Optional.of(sampleMenu(storeId)));
        givenStoreExists(storeId);

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> optionGroupService.createOptionGroup(menuId, request, managerUser()));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_OPTION_SELECT_RANGE);
        verify(optionGroupRepository, never()).save(any());
    }

    @Test
    void 옵션그룹생성_필수그룹인데_minSelect_미입력이면_기본값0이_적용되어_INVALID_OPTION_SELECT_RANGE를_던진다() {
        // given
        UUID menuId = UUID.randomUUID();
        MenuOptionGroupCreateRequest request =
                new MenuOptionGroupCreateRequest("음료 선택", true, null, 1, 1, true);
        UUID storeId = UUID.randomUUID();
        given(menuRepository.findByIdAndDeletedAtIsNull(menuId))
                .willReturn(Optional.of(sampleMenu(storeId)));
        givenStoreExists(storeId);

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> optionGroupService.createOptionGroup(menuId, request, managerUser()));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_OPTION_SELECT_RANGE);
        verify(optionGroupRepository, never()).save(any());
    }

    @Test
    void 옵션그룹생성_OWNER가_본인가게면_성공한다() {
        // given
        UUID menuId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UserDetailsImpl owner = ownerUser(1L);
        MenuOptionGroupCreateRequest request =
                new MenuOptionGroupCreateRequest("음료 선택", true, 1, 1, 1, true);
        MenuOptionGroup saved = sampleGroup(menuId);
        ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
        given(menuRepository.findByIdAndDeletedAtIsNull(menuId))
                .willReturn(Optional.of(sampleMenu(storeId)));
        givenStoreExists(storeId);
        givenOwnerOwnsStore(storeId, owner.userId());
        given(optionGroupRepository.save(any(MenuOptionGroup.class))).willReturn(saved);

        // when
        MenuOptionGroupCreateResponse result = optionGroupService.createOptionGroup(menuId, request, owner);

        // then
        assertThat(result.optionGroupId()).isEqualTo(saved.getId());
    }

    @Test
    void 옵션그룹생성_CUSTOMER면_ACCESS_DENIED를_던진다() {
        // given
        UUID menuId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        MenuOptionGroupCreateRequest request =
                new MenuOptionGroupCreateRequest("음료 선택", true, 1, 1, 1, true);
        given(menuRepository.findByIdAndDeletedAtIsNull(menuId))
                .willReturn(Optional.of(sampleMenu(storeId)));

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> optionGroupService.createOptionGroup(menuId, request, customerUser()));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
        verify(storeRepository, never()).existsByIdAndDeletedAtIsNull(any());
        verify(optionGroupRepository, never()).save(any());
    }

    @Test
    void 옵션그룹생성_없는가게면_STORE_NOT_FOUND를_던진다() {
        // given
        UUID menuId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        MenuOptionGroupCreateRequest request =
                new MenuOptionGroupCreateRequest("음료 선택", true, 1, 1, 1, true);
        given(menuRepository.findByIdAndDeletedAtIsNull(menuId))
                .willReturn(Optional.of(sampleMenu(storeId)));

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> optionGroupService.createOptionGroup(menuId, request, masterUser()));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.STORE_NOT_FOUND);
        verify(optionGroupRepository, never()).save(any());
    }

    @Test
    void 옵션그룹생성_MASTER면_성공한다() {
        // given
        UUID menuId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        MenuOptionGroupCreateRequest request =
                new MenuOptionGroupCreateRequest("음료 선택", true, 1, 1, 1, true);
        MenuOptionGroup saved = sampleGroup(menuId);
        ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
        given(menuRepository.findByIdAndDeletedAtIsNull(menuId))
                .willReturn(Optional.of(sampleMenu(storeId)));
        givenStoreExists(storeId);
        given(optionGroupRepository.save(any(MenuOptionGroup.class))).willReturn(saved);

        // when
        MenuOptionGroupCreateResponse result =
                optionGroupService.createOptionGroup(menuId, request, masterUser());

        // then
        assertThat(result.optionGroupId()).isEqualTo(saved.getId());
        verify(storeRepository, never())
                .existsByIdAndOwner_IdAndDeletedAtIsNull(any(), any());
    }

    @Test
    void 옵션그룹단건조회_성공하면_MenuOptionGroupResponse를_반환한다() {
        // given
        UUID id = UUID.randomUUID();
        UUID menuId = UUID.randomUUID();
        MenuOptionGroup group = sampleGroup(menuId);
        ReflectionTestUtils.setField(group, "id", id);
        given(optionGroupRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.of(group));

        // when
        MenuOptionGroupResponse result = optionGroupService.getOptionGroup(id);

        // then
        assertThat(result.optionGroupId()).isEqualTo(id);
        assertThat(result.menuId()).isEqualTo(menuId);
        assertThat(result.name()).isEqualTo("음료 선택");
        assertThat(result.minSelect()).isEqualTo(1);
        assertThat(result.maxSelect()).isEqualTo(1);
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void 옵션그룹단건조회_없는id는_OPTION_GROUP_NOT_FOUND를_던진다() {
        // given
        UUID id = UUID.randomUUID();
        given(optionGroupRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> optionGroupService.getOptionGroup(id));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.OPTION_GROUP_NOT_FOUND);
    }

    @Test
    void 옵션그룹목록조회_성공하면_페이징된_MenuOptionGroupResponse를_반환한다() {
        // given
        UUID menuId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        UUID id = UUID.randomUUID();
        MenuOptionGroup group = sampleGroup(menuId);
        ReflectionTestUtils.setField(group, "id", id);
        Page<MenuOptionGroup> page = new PageImpl<>(List.of(group), pageable, 1);

        given(optionGroupRepository.searchOptionGroups(menuId, "음료", true, pageable)).willReturn(page);

        // when
        Page<MenuOptionGroupResponse> result =
                optionGroupService.getOptionGroupList(menuId, "음료", true, pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).optionGroupId()).isEqualTo(id);
        assertThat(result.getContent().get(0).name()).isEqualTo("음료 선택");
    }

    @Test
    void 옵션그룹목록조회_keyword가_null이면_빈문자열로_검색한다() {
        // given
        UUID menuId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Page<MenuOptionGroup> page = new PageImpl<>(List.of(), pageable, 0);
        given(optionGroupRepository.searchOptionGroups(menuId, "", true, pageable)).willReturn(page);

        // when
        optionGroupService.getOptionGroupList(menuId, null, null, pageable);

        // then
        verify(optionGroupRepository).searchOptionGroups(menuId, "", true, pageable);
    }

    @Test
    void 옵션그룹수정_성공하면_수정된_MenuOptionGroupResponse를_반환한다() {
        // given
        UUID id = UUID.randomUUID();
        UUID menuId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        MenuOptionGroup group = sampleGroup(menuId);
        ReflectionTestUtils.setField(group, "id", id);
        MenuOptionGroupUpdateRequest request =
                new MenuOptionGroupUpdateRequest("사이드 선택", false, 0, 2, 2, false);

        given(optionGroupRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.of(group));
        given(menuRepository.findByIdAndDeletedAtIsNull(menuId)).willReturn(Optional.of(sampleMenu(storeId)));
        givenStoreExists(storeId);

        // when
        MenuOptionGroupResponse result = optionGroupService.updateOptionGroup(id, request, managerUser());

        // then
        assertThat(result.name()).isEqualTo("사이드 선택");
        assertThat(result.isRequired()).isFalse();
        assertThat(result.minSelect()).isEqualTo(0);
        assertThat(result.maxSelect()).isEqualTo(2);
        assertThat(result.sortOrder()).isEqualTo(2);
        assertThat(result.isActive()).isFalse();
    }

    @Test
    void 옵션그룹수정_없는id는_OPTION_GROUP_NOT_FOUND를_던진다() {
        // given
        UUID id = UUID.randomUUID();
        MenuOptionGroupUpdateRequest request =
                new MenuOptionGroupUpdateRequest("사이드 선택", null, null, null, null, null);
        given(optionGroupRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> optionGroupService.updateOptionGroup(id, request, managerUser()));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.OPTION_GROUP_NOT_FOUND);
    }

    @Test
    void 옵션그룹수정_minSelect가_maxSelect보다_크면_INVALID_OPTION_SELECT_RANGE를_던진다() {
        // given
        UUID id = UUID.randomUUID();
        UUID menuId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        MenuOptionGroup group = sampleGroup(menuId);
        ReflectionTestUtils.setField(group, "id", id);
        MenuOptionGroupUpdateRequest request =
                new MenuOptionGroupUpdateRequest(null, null, 3, 2, null, null);

        given(optionGroupRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.of(group));
        given(menuRepository.findByIdAndDeletedAtIsNull(menuId)).willReturn(Optional.of(sampleMenu(storeId)));
        givenStoreExists(storeId);

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> optionGroupService.updateOptionGroup(id, request, managerUser()));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_OPTION_SELECT_RANGE);
    }

    @Test
    void 옵션그룹수정_필수그룹인데_minSelect를_0으로_바꾸면_INVALID_OPTION_SELECT_RANGE를_던진다() {
        // given
        UUID id = UUID.randomUUID();
        UUID menuId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        MenuOptionGroup group = sampleGroup(menuId);
        ReflectionTestUtils.setField(group, "id", id);
        MenuOptionGroupUpdateRequest request =
                new MenuOptionGroupUpdateRequest(null, null, 0, null, null, null);

        given(optionGroupRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.of(group));
        given(menuRepository.findByIdAndDeletedAtIsNull(menuId)).willReturn(Optional.of(sampleMenu(storeId)));
        givenStoreExists(storeId);

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> optionGroupService.updateOptionGroup(id, request, managerUser()));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_OPTION_SELECT_RANGE);
    }

    @Test
    void 옵션그룹수정_OWNER가_본인가게가_아니면_NO_OPTION_GROUP_PERMISSION을_던진다() {
        // given
        UUID id = UUID.randomUUID();
        UUID menuId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UserDetailsImpl owner = ownerUser(1L);
        MenuOptionGroup group = sampleGroup(menuId);
        ReflectionTestUtils.setField(group, "id", id);
        MenuOptionGroupUpdateRequest request =
                new MenuOptionGroupUpdateRequest("사이드 선택", null, null, null, null, null);

        given(optionGroupRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.of(group));
        given(menuRepository.findByIdAndDeletedAtIsNull(menuId)).willReturn(Optional.of(sampleMenu(storeId)));
        givenStoreExists(storeId);

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> optionGroupService.updateOptionGroup(id, request, owner));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NO_OPTION_GROUP_PERMISSION);
    }

    @Test
    void 옵션그룹삭제_성공하면_deletedAt을_반환하고_소프트삭제된다() {
        // given
        UUID id = UUID.randomUUID();
        Long userId = 1L;
        UserDetailsImpl user = managerUser();
        UUID menuId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        MenuOptionGroup group = sampleGroup(menuId);
        ReflectionTestUtils.setField(group, "id", id);

        given(optionGroupRepository.findById(id)).willReturn(Optional.of(group));
        given(menuRepository.findByIdAndDeletedAtIsNull(menuId)).willReturn(Optional.of(sampleMenu(storeId)));
        givenStoreExists(storeId);
        given(optionRepository.findAllByOptionGroupIdAndDeletedAtIsNull(id)).willReturn(List.of());

        // when
        MenuOptionGroupDeleteResponse result = optionGroupService.deleteOptionGroup(id, user);

        // then
        assertThat(result.optionGroupId()).isEqualTo(id);
        assertThat(result.deletedAt()).isNotNull();
        assertThat(group.isDeleted()).isTrue();
        assertThat(group.getDeletedAt()).isEqualTo(result.deletedAt());
        assertThat(group.getDeletedBy()).isEqualTo(userId);
    }

    @Test
    void 옵션그룹삭제_성공하면_하위옵션도_소프트삭제된다() {
        // given
        UUID id = UUID.randomUUID();
        Long userId = 1L;
        UserDetailsImpl user = managerUser();
        UUID menuId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        MenuOptionGroup group = sampleGroup(menuId);
        MenuOption option = sampleOption(id);
        ReflectionTestUtils.setField(group, "id", id);
        ReflectionTestUtils.setField(option, "id", UUID.randomUUID());

        given(optionGroupRepository.findById(id)).willReturn(Optional.of(group));
        given(menuRepository.findByIdAndDeletedAtIsNull(menuId)).willReturn(Optional.of(sampleMenu(storeId)));
        givenStoreExists(storeId);
        given(optionRepository.findAllByOptionGroupIdAndDeletedAtIsNull(id)).willReturn(List.of(option));

        // when
        optionGroupService.deleteOptionGroup(id, user);

        // then
        assertThat(group.isDeleted()).isTrue();
        assertThat(option.isDeleted()).isTrue();
        assertThat(option.getDeletedBy()).isEqualTo(userId);
    }

    @Test
    void 옵션그룹삭제_없는id는_OPTION_GROUP_NOT_FOUND를_던진다() {
        // given
        UUID id = UUID.randomUUID();
        given(optionGroupRepository.findById(id)).willReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> optionGroupService.deleteOptionGroup(id, managerUser()));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.OPTION_GROUP_NOT_FOUND);
    }

    @Test
    void 옵션그룹삭제_이미삭제된_옵션그룹이면_ALREADY_DELETED_OPTION_GROUP을_던진다() {
        // given
        UUID id = UUID.randomUUID();
        MenuOptionGroup group = sampleGroup(UUID.randomUUID());
        ReflectionTestUtils.setField(group, "id", id);
        ReflectionTestUtils.setField(group, "deletedAt", LocalDateTime.now());

        given(optionGroupRepository.findById(id)).willReturn(Optional.of(group));

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> optionGroupService.deleteOptionGroup(id, managerUser()));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ALREADY_DELETED_OPTION_GROUP);
    }

    @Test
    void 옵션그룹생성_OWNER가_본인가게가_아니면_NO_OPTION_GROUP_PERMISSION을_던진다() {
        // given
        UUID menuId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        MenuOptionGroupCreateRequest request =
                new MenuOptionGroupCreateRequest("음료 선택", true, 1, 1, 1, true);
        given(menuRepository.findByIdAndDeletedAtIsNull(menuId))
                .willReturn(Optional.of(sampleMenu(storeId)));
        givenStoreExists(storeId);

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> optionGroupService.createOptionGroup(menuId, request, ownerUser(1L)));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NO_OPTION_GROUP_PERMISSION);
        verify(optionGroupRepository, never()).save(any());
    }

    @Test
    void 옵션그룹삭제_OWNER가_본인가게가_아니면_NO_OPTION_GROUP_PERMISSION을_던진다() {
        // given
        UUID optionGroupId = UUID.randomUUID();
        UUID menuId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        MenuOptionGroup group = sampleGroup(menuId);
        ReflectionTestUtils.setField(group, "id", optionGroupId);
        given(optionGroupRepository.findById(optionGroupId)).willReturn(Optional.of(group));
        given(menuRepository.findByIdAndDeletedAtIsNull(menuId))
                .willReturn(Optional.of(sampleMenu(storeId)));
        givenStoreExists(storeId);

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> optionGroupService.deleteOptionGroup(optionGroupId, ownerUser(1L)));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NO_OPTION_GROUP_PERMISSION);
        assertThat(group.isDeleted()).isFalse();
        verifyNoInteractions(optionRepository);
    }
}
