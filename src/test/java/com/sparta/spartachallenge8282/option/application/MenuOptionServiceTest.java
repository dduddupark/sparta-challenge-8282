package com.sparta.spartachallenge8282.option.application;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import com.sparta.spartachallenge8282.menu.domain.Menu;
import com.sparta.spartachallenge8282.menu.domain.MenuBadge;
import com.sparta.spartachallenge8282.menu.domain.MenuRepository;
import com.sparta.spartachallenge8282.menu.domain.MenuStatus;
import com.sparta.spartachallenge8282.option.domain.MenuOption;
import com.sparta.spartachallenge8282.option.domain.MenuOptionRepository;
import com.sparta.spartachallenge8282.option.presentation.dto.request.MenuOptionCreateRequest;
import com.sparta.spartachallenge8282.option.presentation.dto.request.MenuOptionUpdateRequest;
import com.sparta.spartachallenge8282.option.presentation.dto.response.MenuOptionCreateResponse;
import com.sparta.spartachallenge8282.option.presentation.dto.response.MenuOptionDeleteResponse;
import com.sparta.spartachallenge8282.option.presentation.dto.response.MenuOptionResponse;
import com.sparta.spartachallenge8282.optiongroup.domain.MenuOptionGroup;
import com.sparta.spartachallenge8282.optiongroup.domain.MenuOptionGroupRepository;
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

@ExtendWith(MockitoExtension.class)
class MenuOptionServiceTest {

    @Mock
    private MenuOptionRepository optionRepository;

    @Mock
    private MenuOptionGroupRepository optionGroupRepository;

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private MenuOptionService optionService;

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
    void 옵션생성_성공하면_생성된_id를_반환한다() {
        // given
        UUID optionGroupId = UUID.randomUUID();
        UUID menuId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        MenuOptionCreateRequest request = new MenuOptionCreateRequest("콜라", 1000, 1, true);

        UUID generatedId = UUID.randomUUID();
        MenuOption saved = sampleOption(optionGroupId);
        ReflectionTestUtils.setField(saved, "id", generatedId);

        given(optionGroupRepository.findByIdAndDeletedAtIsNull(optionGroupId))
                .willReturn(Optional.of(sampleGroup(menuId)));
        given(menuRepository.findByIdAndDeletedAtIsNull(menuId)).willReturn(Optional.of(sampleMenu(storeId)));
        givenStoreExists(storeId);
        given(optionRepository.save(any(MenuOption.class))).willReturn(saved);

        // when
        MenuOptionCreateResponse result = optionService.createOption(optionGroupId, request, managerUser());

        // then
        assertThat(result.optionId()).isEqualTo(generatedId);
    }

    @Test
    void 옵션생성_존재하지_않는_옵션그룹이면_OPTION_GROUP_NOT_FOUND를_던진다() {
        // given
        UUID optionGroupId = UUID.randomUUID();
        MenuOptionCreateRequest request = new MenuOptionCreateRequest("콜라", 1000, 1, true);
        given(optionGroupRepository.findByIdAndDeletedAtIsNull(optionGroupId)).willReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> optionService.createOption(optionGroupId, request, managerUser()));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.OPTION_GROUP_NOT_FOUND);
        verify(optionRepository, never()).save(any());
    }

    @Test
    void 옵션생성_가격이_음수면_INVALID_OPTION_PRICE를_던진다() {
        // given
        UUID optionGroupId = UUID.randomUUID();
        UUID menuId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        MenuOptionCreateRequest request = new MenuOptionCreateRequest("콜라", -1000, 1, true);
        given(optionGroupRepository.findByIdAndDeletedAtIsNull(optionGroupId))
                .willReturn(Optional.of(sampleGroup(menuId)));
        given(menuRepository.findByIdAndDeletedAtIsNull(menuId)).willReturn(Optional.of(sampleMenu(storeId)));
        givenStoreExists(storeId);

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> optionService.createOption(optionGroupId, request, managerUser()));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_OPTION_PRICE);
        verify(optionRepository, never()).save(any());
    }

    @Test
    void 옵션생성_OWNER가_본인가게면_성공한다() {
        // given
        UUID optionGroupId = UUID.randomUUID();
        UUID menuId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UserDetailsImpl owner = ownerUser(1L);
        MenuOptionCreateRequest request = new MenuOptionCreateRequest("콜라", 1000, 1, true);
        MenuOption saved = sampleOption(optionGroupId);
        ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
        given(optionGroupRepository.findByIdAndDeletedAtIsNull(optionGroupId))
                .willReturn(Optional.of(sampleGroup(menuId)));
        given(menuRepository.findByIdAndDeletedAtIsNull(menuId))
                .willReturn(Optional.of(sampleMenu(storeId)));
        givenStoreExists(storeId);
        givenOwnerOwnsStore(storeId, owner.userId());
        given(optionRepository.save(any(MenuOption.class))).willReturn(saved);

        // when
        MenuOptionCreateResponse result = optionService.createOption(optionGroupId, request, owner);

        // then
        assertThat(result.optionId()).isEqualTo(saved.getId());
    }

    @Test
    void 옵션생성_CUSTOMER면_ACCESS_DENIED를_던진다() {
        // given
        UUID optionGroupId = UUID.randomUUID();
        UUID menuId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        MenuOptionCreateRequest request = new MenuOptionCreateRequest("콜라", 1000, 1, true);
        given(optionGroupRepository.findByIdAndDeletedAtIsNull(optionGroupId))
                .willReturn(Optional.of(sampleGroup(menuId)));
        given(menuRepository.findByIdAndDeletedAtIsNull(menuId))
                .willReturn(Optional.of(sampleMenu(storeId)));

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> optionService.createOption(optionGroupId, request, customerUser()));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
        verify(storeRepository, never()).existsByIdAndDeletedAtIsNull(any());
        verify(optionRepository, never()).save(any());
    }

    @Test
    void 옵션생성_없는가게면_STORE_NOT_FOUND를_던진다() {
        // given
        UUID optionGroupId = UUID.randomUUID();
        UUID menuId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        MenuOptionCreateRequest request = new MenuOptionCreateRequest("콜라", 1000, 1, true);
        given(optionGroupRepository.findByIdAndDeletedAtIsNull(optionGroupId))
                .willReturn(Optional.of(sampleGroup(menuId)));
        given(menuRepository.findByIdAndDeletedAtIsNull(menuId))
                .willReturn(Optional.of(sampleMenu(storeId)));

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> optionService.createOption(optionGroupId, request, masterUser()));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.STORE_NOT_FOUND);
        verify(optionRepository, never()).save(any());
    }

    @Test
    void 옵션생성_MASTER면_성공한다() {
        // given
        UUID optionGroupId = UUID.randomUUID();
        UUID menuId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        MenuOptionCreateRequest request = new MenuOptionCreateRequest("콜라", 1000, 1, true);
        MenuOption saved = sampleOption(optionGroupId);
        ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
        given(optionGroupRepository.findByIdAndDeletedAtIsNull(optionGroupId))
                .willReturn(Optional.of(sampleGroup(menuId)));
        given(menuRepository.findByIdAndDeletedAtIsNull(menuId))
                .willReturn(Optional.of(sampleMenu(storeId)));
        givenStoreExists(storeId);
        given(optionRepository.save(any(MenuOption.class))).willReturn(saved);

        // when
        MenuOptionCreateResponse result = optionService.createOption(optionGroupId, request, masterUser());

        // then
        assertThat(result.optionId()).isEqualTo(saved.getId());
        verify(storeRepository, never())
                .existsByIdAndOwner_IdAndDeletedAtIsNull(any(), any());
    }

    @Test
    void 옵션단건조회_성공하면_MenuOptionResponse를_반환한다() {
        // given
        UUID id = UUID.randomUUID();
        UUID optionGroupId = UUID.randomUUID();
        MenuOption option = sampleOption(optionGroupId);
        ReflectionTestUtils.setField(option, "id", id);
        given(optionRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.of(option));

        // when
        MenuOptionResponse result = optionService.getOption(id);

        // then
        assertThat(result.optionId()).isEqualTo(id);
        assertThat(result.optionGroupId()).isEqualTo(optionGroupId);
        assertThat(result.name()).isEqualTo("콜라");
        assertThat(result.additionalPrice()).isEqualTo(1000);
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void 옵션단건조회_없는id는_OPTION_NOT_FOUND를_던진다() {
        // given
        UUID id = UUID.randomUUID();
        given(optionRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> optionService.getOption(id));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.OPTION_NOT_FOUND);
    }

    @Test
    void 옵션목록조회_성공하면_페이징된_MenuOptionResponse를_반환한다() {
        // given
        UUID optionGroupId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        UUID id = UUID.randomUUID();
        MenuOption option = sampleOption(optionGroupId);
        ReflectionTestUtils.setField(option, "id", id);
        Page<MenuOption> page = new PageImpl<>(List.of(option), pageable, 1);

        given(optionRepository.searchOptions(optionGroupId, "콜라", true, pageable)).willReturn(page);

        // when
        Page<MenuOptionResponse> result =
                optionService.getOptionList(optionGroupId, "콜라", true, pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).optionId()).isEqualTo(id);
        assertThat(result.getContent().get(0).name()).isEqualTo("콜라");
    }

    @Test
    void 옵션목록조회_keyword가_null이면_빈문자열로_검색한다() {
        // given
        UUID optionGroupId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Page<MenuOption> page = new PageImpl<>(List.of(), pageable, 0);
        given(optionRepository.searchOptions(optionGroupId, "", true, pageable)).willReturn(page);

        // when
        optionService.getOptionList(optionGroupId, null, null, pageable);

        // then
        verify(optionRepository).searchOptions(optionGroupId, "", true, pageable);
    }

    @Test
    void 옵션수정_성공하면_수정된_MenuOptionResponse를_반환한다() {
        // given
        UUID id = UUID.randomUUID();
        UUID optionGroupId = UUID.randomUUID();
        UUID menuId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        MenuOption option = sampleOption(optionGroupId);
        ReflectionTestUtils.setField(option, "id", id);
        MenuOptionUpdateRequest request = new MenuOptionUpdateRequest("사이다", 500, 2, false);

        given(optionRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.of(option));
        given(optionGroupRepository.findByIdAndDeletedAtIsNull(optionGroupId)).willReturn(Optional.of(sampleGroup(menuId)));
        given(menuRepository.findByIdAndDeletedAtIsNull(menuId)).willReturn(Optional.of(sampleMenu(storeId)));
        givenStoreExists(storeId);

        // when
        MenuOptionResponse result = optionService.updateOption(id, request, managerUser());

        // then
        assertThat(result.name()).isEqualTo("사이다");
        assertThat(result.additionalPrice()).isEqualTo(500);
        assertThat(result.sortOrder()).isEqualTo(2);
        assertThat(result.isActive()).isFalse();
    }

    @Test
    void 옵션수정_없는id는_OPTION_NOT_FOUND를_던진다() {
        // given
        UUID id = UUID.randomUUID();
        MenuOptionUpdateRequest request = new MenuOptionUpdateRequest("사이다", null, null, null);
        given(optionRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> optionService.updateOption(id, request, managerUser()));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.OPTION_NOT_FOUND);
    }

    @Test
    void 옵션수정_가격이_음수면_INVALID_OPTION_PRICE를_던진다() {
        // given
        UUID id = UUID.randomUUID();
        UUID optionGroupId = UUID.randomUUID();
        UUID menuId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        MenuOption option = sampleOption(optionGroupId);
        ReflectionTestUtils.setField(option, "id", id);
        MenuOptionUpdateRequest request = new MenuOptionUpdateRequest(null, -500, null, null);

        given(optionRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.of(option));
        given(optionGroupRepository.findByIdAndDeletedAtIsNull(optionGroupId)).willReturn(Optional.of(sampleGroup(menuId)));
        given(menuRepository.findByIdAndDeletedAtIsNull(menuId)).willReturn(Optional.of(sampleMenu(storeId)));
        givenStoreExists(storeId);

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> optionService.updateOption(id, request, managerUser()));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_OPTION_PRICE);
    }

    @Test
    void 옵션수정_OWNER가_본인가게가_아니면_NO_OPTION_PERMISSION을_던진다() {
        // given
        UUID id = UUID.randomUUID();
        UUID optionGroupId = UUID.randomUUID();
        UUID menuId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UserDetailsImpl owner = ownerUser(1L);
        MenuOption option = sampleOption(optionGroupId);
        ReflectionTestUtils.setField(option, "id", id);
        MenuOptionUpdateRequest request = new MenuOptionUpdateRequest("사이다", null, null, null);

        given(optionRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.of(option));
        given(optionGroupRepository.findByIdAndDeletedAtIsNull(optionGroupId)).willReturn(Optional.of(sampleGroup(menuId)));
        given(menuRepository.findByIdAndDeletedAtIsNull(menuId)).willReturn(Optional.of(sampleMenu(storeId)));
        givenStoreExists(storeId);

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> optionService.updateOption(id, request, owner));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NO_OPTION_PERMISSION);
    }

    @Test
    void 옵션삭제_성공하면_deletedAt을_반환하고_소프트삭제된다() {
        // given
        UUID id = UUID.randomUUID();
        Long userId = 1L;
        UserDetailsImpl user = managerUser();
        UUID optionGroupId = UUID.randomUUID();
        UUID menuId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        MenuOption option = sampleOption(optionGroupId);
        ReflectionTestUtils.setField(option, "id", id);

        given(optionRepository.findById(id)).willReturn(Optional.of(option));
        given(optionGroupRepository.findByIdAndDeletedAtIsNull(optionGroupId)).willReturn(Optional.of(sampleGroup(menuId)));
        given(menuRepository.findByIdAndDeletedAtIsNull(menuId)).willReturn(Optional.of(sampleMenu(storeId)));
        givenStoreExists(storeId);

        // when
        MenuOptionDeleteResponse result = optionService.deleteOption(id, user);

        // then
        assertThat(result.optionId()).isEqualTo(id);
        assertThat(result.deletedAt()).isNotNull();
        assertThat(option.isDeleted()).isTrue();
        assertThat(option.getDeletedAt()).isEqualTo(result.deletedAt());
        assertThat(option.getDeletedBy()).isEqualTo(userId);
    }

    @Test
    void 옵션삭제_없는id는_OPTION_NOT_FOUND를_던진다() {
        // given
        UUID id = UUID.randomUUID();
        given(optionRepository.findById(id)).willReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> optionService.deleteOption(id, managerUser()));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.OPTION_NOT_FOUND);
    }

    @Test
    void 옵션삭제_이미삭제된_옵션이면_ALREADY_DELETED_OPTION을_던진다() {
        // given
        UUID id = UUID.randomUUID();
        MenuOption option = sampleOption(UUID.randomUUID());
        ReflectionTestUtils.setField(option, "id", id);
        ReflectionTestUtils.setField(option, "deletedAt", LocalDateTime.now());

        given(optionRepository.findById(id)).willReturn(Optional.of(option));

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> optionService.deleteOption(id, managerUser()));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ALREADY_DELETED_OPTION);
    }

    @Test
    void 옵션생성_OWNER가_본인가게가_아니면_NO_OPTION_PERMISSION을_던진다() {
        // given
        UUID optionGroupId = UUID.randomUUID();
        UUID menuId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        MenuOptionCreateRequest request = new MenuOptionCreateRequest("콜라", 1000, 1, true);
        given(optionGroupRepository.findByIdAndDeletedAtIsNull(optionGroupId))
                .willReturn(Optional.of(sampleGroup(menuId)));
        given(menuRepository.findByIdAndDeletedAtIsNull(menuId))
                .willReturn(Optional.of(sampleMenu(storeId)));
        givenStoreExists(storeId);

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> optionService.createOption(optionGroupId, request, ownerUser(1L)));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NO_OPTION_PERMISSION);
        verify(optionRepository, never()).save(any());
    }

    @Test
    void 옵션삭제_OWNER가_본인가게가_아니면_NO_OPTION_PERMISSION을_던진다() {
        // given
        UUID optionId = UUID.randomUUID();
        UUID optionGroupId = UUID.randomUUID();
        UUID menuId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        MenuOption option = sampleOption(optionGroupId);
        ReflectionTestUtils.setField(option, "id", optionId);
        given(optionRepository.findById(optionId)).willReturn(Optional.of(option));
        given(optionGroupRepository.findByIdAndDeletedAtIsNull(optionGroupId))
                .willReturn(Optional.of(sampleGroup(menuId)));
        given(menuRepository.findByIdAndDeletedAtIsNull(menuId))
                .willReturn(Optional.of(sampleMenu(storeId)));
        givenStoreExists(storeId);

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> optionService.deleteOption(optionId, ownerUser(1L)));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NO_OPTION_PERMISSION);
        assertThat(option.isDeleted()).isFalse();
    }
}
