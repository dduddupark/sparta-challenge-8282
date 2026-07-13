package com.sparta.spartachallenge8282.menu.option.application;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.menu.option.domain.MenuOption;
import com.sparta.spartachallenge8282.menu.option.domain.MenuOptionRepository;
import com.sparta.spartachallenge8282.menu.option.presentation.dto.request.MenuOptionCreateRequest;
import com.sparta.spartachallenge8282.menu.option.presentation.dto.request.MenuOptionUpdateRequest;
import com.sparta.spartachallenge8282.menu.option.presentation.dto.response.MenuOptionResponse;
import com.sparta.spartachallenge8282.menu.optiongroup.domain.MenuOptionGroup;
import com.sparta.spartachallenge8282.menu.optiongroup.domain.MenuOptionGroupRepository;
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

    @InjectMocks
    private MenuOptionService optionService;

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
    void 옵션생성_성공하면_생성된_id를_반환한다() {
        // given
        UUID optionGroupId = UUID.randomUUID();
        MenuOptionCreateRequest request = new MenuOptionCreateRequest("콜라", 1000, 1, true);

        UUID generatedId = UUID.randomUUID();
        MenuOption saved = sampleOption(optionGroupId);
        ReflectionTestUtils.setField(saved, "id", generatedId);

        given(optionGroupRepository.findByIdAndDeletedAtIsNull(optionGroupId))
                .willReturn(Optional.of(sampleGroup(UUID.randomUUID())));
        given(optionRepository.save(any(MenuOption.class))).willReturn(saved);

        // when
        UUID result = optionService.createOption(optionGroupId, request);

        // then
        assertThat(result).isEqualTo(generatedId);
    }

    @Test
    void 옵션생성_존재하지_않는_옵션그룹이면_OPTION_GROUP_NOT_FOUND를_던진다() {
        // given
        UUID optionGroupId = UUID.randomUUID();
        MenuOptionCreateRequest request = new MenuOptionCreateRequest("콜라", 1000, 1, true);
        given(optionGroupRepository.findByIdAndDeletedAtIsNull(optionGroupId)).willReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> optionService.createOption(optionGroupId, request));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.OPTION_GROUP_NOT_FOUND);
        verify(optionRepository, never()).save(any());
    }

    @Test
    void 옵션생성_가격이_음수면_INVALID_OPTION_PRICE를_던진다() {
        // given
        UUID optionGroupId = UUID.randomUUID();
        MenuOptionCreateRequest request = new MenuOptionCreateRequest("콜라", -1000, 1, true);
        given(optionGroupRepository.findByIdAndDeletedAtIsNull(optionGroupId))
                .willReturn(Optional.of(sampleGroup(UUID.randomUUID())));

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> optionService.createOption(optionGroupId, request));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_OPTION_PRICE);
        verify(optionRepository, never()).save(any());
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
        MenuOption option = sampleOption(UUID.randomUUID());
        ReflectionTestUtils.setField(option, "id", id);
        MenuOptionUpdateRequest request = new MenuOptionUpdateRequest("사이다", 500, 2, false);

        given(optionRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.of(option));

        // when
        MenuOptionResponse result = optionService.updateOption(id, request);

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
                () -> optionService.updateOption(id, request));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.OPTION_NOT_FOUND);
    }

    @Test
    void 옵션수정_가격이_음수면_INVALID_OPTION_PRICE를_던진다() {
        // given
        UUID id = UUID.randomUUID();
        MenuOption option = sampleOption(UUID.randomUUID());
        ReflectionTestUtils.setField(option, "id", id);
        MenuOptionUpdateRequest request = new MenuOptionUpdateRequest(null, -500, null, null);

        given(optionRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.of(option));

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> optionService.updateOption(id, request));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_OPTION_PRICE);
    }

    @Test
    void 옵션삭제_성공하면_deletedAt을_반환하고_소프트삭제된다() {
        // given
        UUID id = UUID.randomUUID();
        Long userId = 1L;
        MenuOption option = sampleOption(UUID.randomUUID());
        ReflectionTestUtils.setField(option, "id", id);

        given(optionRepository.findById(id)).willReturn(Optional.of(option));

        // when
        LocalDateTime deletedAt = optionService.deleteOption(id, userId);

        // then
        assertThat(deletedAt).isNotNull();
        assertThat(option.isDeleted()).isTrue();
        assertThat(option.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(option.getDeletedBy()).isEqualTo(userId);
    }

    @Test
    void 옵션삭제_없는id는_OPTION_NOT_FOUND를_던진다() {
        // given
        UUID id = UUID.randomUUID();
        given(optionRepository.findById(id)).willReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> optionService.deleteOption(id, 1L));

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
                () -> optionService.deleteOption(id, 1L));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ALREADY_DELETED_OPTION);
    }
}
