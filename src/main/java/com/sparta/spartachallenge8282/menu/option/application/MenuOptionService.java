package com.sparta.spartachallenge8282.menu.option.application;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.global.common.PageableUtil;
import com.sparta.spartachallenge8282.menu.option.domain.MenuOption;
import com.sparta.spartachallenge8282.menu.option.domain.MenuOptionRepository;
import com.sparta.spartachallenge8282.menu.option.presentation.dto.request.MenuOptionCreateRequest;
import com.sparta.spartachallenge8282.menu.option.presentation.dto.request.MenuOptionUpdateRequest;
import com.sparta.spartachallenge8282.menu.option.presentation.dto.response.MenuOptionResponse;
import com.sparta.spartachallenge8282.menu.optiongroup.domain.MenuOptionGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 옵션 비즈니스 로직.
 *
 * <p>옵션은 옵션 그룹에 종속 — 생성 시 상위 그룹 존재를 검증한다(OPTION_GROUP_NOT_FOUND).
 * additional_price 음수는 Service(INVALID_OPTION_PRICE) + DB @Check 로 방어.
 * 권한(소유권) 검증은 store 연동(auth 브랜치)에서 구현.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuOptionService {

    private final MenuOptionRepository optionRepository;
    private final MenuOptionGroupRepository optionGroupRepository;

    @Transactional
    public UUID createOption(UUID optionGroupId, MenuOptionCreateRequest request) {
        if (optionGroupRepository.findByIdAndDeletedAtIsNull(optionGroupId).isEmpty()) {
            throw new CustomException(ErrorCode.OPTION_GROUP_NOT_FOUND);
        }
        // TODO(권한, auth 브랜치): OWNER 소유권 확인 → NO_OPTION_PERMISSION
        validatePrice(request.additionalPrice());

        MenuOption option = MenuOption.builder()
                .optionGroupId(optionGroupId)
                .name(request.name())
                .additionalPrice(request.additionalPrice())
                .sortOrder(request.sortOrder())
                .isActive(request.isActive())
                .build();

        return optionRepository.save(option).getId();
    }

    public MenuOptionResponse getOption(UUID id) {
        return MenuOptionResponse.from(findActiveOption(id));
    }

    public Page<MenuOptionResponse> getOptionList(UUID optionGroupId, String keyword, Boolean isActive, Pageable pageable) {
        String searchKeyword = (keyword == null) ? "" : keyword;
        Boolean activeFilter = (isActive == null) ? true : isActive;
        Pageable normalizedPageable = PageableUtil.normalize(pageable);

        return optionRepository.searchOptions(optionGroupId, searchKeyword, activeFilter, normalizedPageable)
                .map(MenuOptionResponse::from);
    }

    @Transactional
    public MenuOptionResponse updateOption(UUID id, MenuOptionUpdateRequest request) {
        MenuOption option = findActiveOption(id);
        // TODO(권한, auth 브랜치): 소유권 검증 → NO_OPTION_PERMISSION
        validatePrice(request.additionalPrice());

        option.updateInfo(request.name(), request.additionalPrice(), request.sortOrder());
        option.changeActive(request.isActive());

        return MenuOptionResponse.from(option);
    }

    @Transactional
    public LocalDateTime deleteOption(UUID id, Long userId) {
        MenuOption option = optionRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.OPTION_NOT_FOUND));

        if (option.isDeleted()) {
            throw new CustomException(ErrorCode.ALREADY_DELETED_OPTION);
        }
        // TODO(권한, auth 브랜치): 소유권 검증
        option.softDelete(userId);
        return option.getDeletedAt();
    }

    private MenuOption findActiveOption(UUID id) {
        return optionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new CustomException(ErrorCode.OPTION_NOT_FOUND));
    }

    private void validatePrice(Integer additionalPrice) {
        if (additionalPrice != null && additionalPrice < 0) {
            throw new CustomException(ErrorCode.INVALID_OPTION_PRICE);
        }
    }
}
