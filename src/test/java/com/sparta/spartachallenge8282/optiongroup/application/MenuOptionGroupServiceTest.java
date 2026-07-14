package com.sparta.spartachallenge8282.optiongroup.application;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
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

@ExtendWith(MockitoExtension.class)
class MenuOptionGroupServiceTest {

    @Mock
    private MenuOptionGroupRepository optionGroupRepository;

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private MenuOptionRepository optionRepository;

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

    @Test
    void 옵션그룹생성_성공하면_생성된_id를_반환한다() {
        // given
        UUID menuId = UUID.randomUUID();
        MenuOptionGroupCreateRequest request =
                new MenuOptionGroupCreateRequest("음료 선택", true, 1, 1, 1, true);

        UUID generatedId = UUID.randomUUID();
        MenuOptionGroup saved = sampleGroup(menuId);
        ReflectionTestUtils.setField(saved, "id", generatedId);

        given(menuRepository.findByIdAndDeletedAtIsNull(menuId))
                .willReturn(Optional.of(sampleMenu(UUID.randomUUID())));
        given(optionGroupRepository.save(any(MenuOptionGroup.class))).willReturn(saved);

        // when
        MenuOptionGroupCreateResponse result = optionGroupService.createOptionGroup(menuId, request);

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
                () -> optionGroupService.createOptionGroup(menuId, request));

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
        given(menuRepository.findByIdAndDeletedAtIsNull(menuId))
                .willReturn(Optional.of(sampleMenu(UUID.randomUUID())));

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> optionGroupService.createOptionGroup(menuId, request));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_OPTION_SELECT_RANGE);
        verify(optionGroupRepository, never()).save(any());
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
        MenuOptionGroup group = sampleGroup(UUID.randomUUID());
        ReflectionTestUtils.setField(group, "id", id);
        MenuOptionGroupUpdateRequest request =
                new MenuOptionGroupUpdateRequest("사이드 선택", false, 0, 2, 2, false);

        given(optionGroupRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.of(group));

        // when
        MenuOptionGroupResponse result = optionGroupService.updateOptionGroup(id, request);

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
                () -> optionGroupService.updateOptionGroup(id, request));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.OPTION_GROUP_NOT_FOUND);
    }

    @Test
    void 옵션그룹수정_minSelect가_maxSelect보다_크면_INVALID_OPTION_SELECT_RANGE를_던진다() {
        // given
        UUID id = UUID.randomUUID();
        MenuOptionGroup group = sampleGroup(UUID.randomUUID());
        ReflectionTestUtils.setField(group, "id", id);
        MenuOptionGroupUpdateRequest request =
                new MenuOptionGroupUpdateRequest(null, null, 3, 2, null, null);

        given(optionGroupRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.of(group));

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> optionGroupService.updateOptionGroup(id, request));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_OPTION_SELECT_RANGE);
    }

    @Test
    void 옵션그룹삭제_성공하면_deletedAt을_반환하고_소프트삭제된다() {
        // given
        UUID id = UUID.randomUUID();
        Long userId = 1L;
        MenuOptionGroup group = sampleGroup(UUID.randomUUID());
        ReflectionTestUtils.setField(group, "id", id);

        given(optionGroupRepository.findById(id)).willReturn(Optional.of(group));
        given(optionRepository.findAllByOptionGroupIdAndDeletedAtIsNull(id)).willReturn(List.of());

        // when
        MenuOptionGroupDeleteResponse result = optionGroupService.deleteOptionGroup(id, userId);

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
        MenuOptionGroup group = sampleGroup(UUID.randomUUID());
        MenuOption option = sampleOption(id);
        ReflectionTestUtils.setField(group, "id", id);
        ReflectionTestUtils.setField(option, "id", UUID.randomUUID());

        given(optionGroupRepository.findById(id)).willReturn(Optional.of(group));
        given(optionRepository.findAllByOptionGroupIdAndDeletedAtIsNull(id)).willReturn(List.of(option));

        // when
        optionGroupService.deleteOptionGroup(id, userId);

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
                () -> optionGroupService.deleteOptionGroup(id, 1L));

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
                () -> optionGroupService.deleteOptionGroup(id, 1L));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ALREADY_DELETED_OPTION_GROUP);
    }
}
