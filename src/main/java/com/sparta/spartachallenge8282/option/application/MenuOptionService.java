package com.sparta.spartachallenge8282.option.application;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.global.common.PageableUtil;
import com.sparta.spartachallenge8282.option.domain.MenuOption;
import com.sparta.spartachallenge8282.option.domain.MenuOptionRepository;
import com.sparta.spartachallenge8282.option.presentation.dto.request.MenuOptionCreateRequest;
import com.sparta.spartachallenge8282.option.presentation.dto.request.MenuOptionUpdateRequest;
import com.sparta.spartachallenge8282.option.presentation.dto.response.MenuOptionCreateResponse;
import com.sparta.spartachallenge8282.option.presentation.dto.response.MenuOptionDeleteResponse;
import com.sparta.spartachallenge8282.option.presentation.dto.response.MenuOptionResponse;
import com.sparta.spartachallenge8282.optiongroup.domain.MenuOptionGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * 옵션 생성.
     *
     * <p>상위 옵션 그룹이 없으면 {@code OPTION_GROUP_NOT_FOUND}, 추가 금액이 음수면 {@code INVALID_OPTION_PRICE}.
     *
     * @param optionGroupId 옵션이 속할 옵션 그룹 ID (경로 변수)
     */
    @Transactional
    public MenuOptionCreateResponse createOption(UUID optionGroupId, MenuOptionCreateRequest request) {
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

        return MenuOptionCreateResponse.from(optionRepository.save(option));
    }

    /** 옵션 단건 조회. (삭제되지 않은 항목이면 비활성이어도 조회된다 — 목록 조회와 필터가 다르다) */
    public MenuOptionResponse getOption(UUID id) {
        return MenuOptionResponse.from(findActiveOption(id));
    }

    /**
     * 옵션 그룹별 옵션 목록 검색. (페이지 크기는 10/30/50 만 허용)
     *
     * <p>{@code isActive} 미지정 시 활성 항목만 조회한다.
     */
    public Page<MenuOptionResponse> getOptionList(UUID optionGroupId, String keyword, Boolean isActive, Pageable pageable) {
        String searchKeyword = (keyword == null) ? "" : keyword;
        Boolean activeFilter = (isActive == null) ? true : isActive;
        Pageable normalizedPageable = PageableUtil.normalize(pageable);

        return optionRepository.searchOptions(optionGroupId, searchKeyword, activeFilter, normalizedPageable)
                .map(MenuOptionResponse::from);
    }

    /** 옵션 수정 (부분 수정 — null 인 필드는 변경하지 않는다). 추가 금액이 음수면 {@code INVALID_OPTION_PRICE}. */
    @Transactional
    public MenuOptionResponse updateOption(UUID id, MenuOptionUpdateRequest request) {
        MenuOption option = findActiveOption(id);
        // TODO(권한, auth 브랜치): 소유권 검증 → NO_OPTION_PERMISSION
        validatePrice(request.additionalPrice());

        option.updateInfo(request.name(), request.additionalPrice(), request.sortOrder());
        option.changeActive(request.isActive());

        return MenuOptionResponse.from(option);
    }

    /**
     * 옵션 삭제 (소프트 삭제).
     *
     * @param userId 삭제를 수행한 사용자 ID ({@code deleted_by} 에 기록)
     */
    @Transactional
    public MenuOptionDeleteResponse deleteOption(UUID id, Long userId) {
        MenuOption option = optionRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.OPTION_NOT_FOUND));

        if (option.isDeleted()) {
            throw new CustomException(ErrorCode.ALREADY_DELETED_OPTION);
        }
        // TODO(권한, auth 브랜치): 소유권 검증
        option.softDelete(userId);
        return MenuOptionDeleteResponse.from(option);
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
