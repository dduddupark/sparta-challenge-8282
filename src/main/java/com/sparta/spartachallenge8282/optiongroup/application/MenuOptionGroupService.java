package com.sparta.spartachallenge8282.optiongroup.application;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.global.common.PageableUtil;
import com.sparta.spartachallenge8282.menu.domain.MenuRepository;
import com.sparta.spartachallenge8282.option.domain.MenuOptionRepository;
import com.sparta.spartachallenge8282.optiongroup.domain.MenuOptionGroup;
import com.sparta.spartachallenge8282.optiongroup.domain.MenuOptionGroupRepository;
import com.sparta.spartachallenge8282.optiongroup.presentation.dto.request.MenuOptionGroupCreateRequest;
import com.sparta.spartachallenge8282.optiongroup.presentation.dto.request.MenuOptionGroupUpdateRequest;
import com.sparta.spartachallenge8282.optiongroup.presentation.dto.response.MenuOptionGroupResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 옵션 그룹 비즈니스 로직.
 *
 * <p>옵션 그룹은 메뉴에 종속 — 생성 시 상위 menu 존재를 검증한다(MENU_NOT_FOUND).
 * min/max 선택 범위는 Service(INVALID_OPTION_SELECT_RANGE) + DB @Check 로 방어.
 * 권한(소유권) 검증은 store 연동(auth 브랜치)에서 구현.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuOptionGroupService {

    private final MenuOptionGroupRepository optionGroupRepository;
    private final MenuRepository menuRepository;
    private final MenuOptionRepository optionRepository;

    @Transactional
    public UUID createOptionGroup(UUID menuId, MenuOptionGroupCreateRequest request) {
        if (menuRepository.findByIdAndDeletedAtIsNull(menuId).isEmpty()) {
            throw new CustomException(ErrorCode.MENU_NOT_FOUND);
        }
        // TODO(권한, auth 브랜치): OWNER 는 menu 의 가게가 본인 가게인지 확인 → NO_OPTION_GROUP_PERMISSION

        MenuOptionGroup group = MenuOptionGroup.builder()
                .menuId(menuId)
                .name(request.name())
                .isRequired(request.isRequired())
                .minSelect(request.minSelect())
                .maxSelect(request.maxSelect())
                .sortOrder(request.sortOrder())
                .isActive(request.isActive())
                .build();
        validateSelectRange(group.getMinSelect(), group.getMaxSelect());

        return optionGroupRepository.save(group).getId();
    }

    public MenuOptionGroupResponse getOptionGroup(UUID id) {
        return MenuOptionGroupResponse.from(findActiveGroup(id));
    }

    public Page<MenuOptionGroupResponse> getOptionGroupList(UUID menuId, String keyword, Boolean isActive, Pageable pageable) {
        String searchKeyword = (keyword == null) ? "" : keyword;
        Boolean activeFilter = (isActive == null) ? true : isActive;
        Pageable normalizedPageable = PageableUtil.normalize(pageable);

        return optionGroupRepository.searchOptionGroups(menuId, searchKeyword, activeFilter, normalizedPageable)
                .map(MenuOptionGroupResponse::from);
    }

    @Transactional
    public MenuOptionGroupResponse updateOptionGroup(UUID id, MenuOptionGroupUpdateRequest request) {
        MenuOptionGroup group = findActiveGroup(id);
        // TODO(권한, auth 브랜치): 소유권 검증 → NO_OPTION_GROUP_PERMISSION

        group.updateInfo(request.name(), request.minSelect(), request.maxSelect(), request.sortOrder());
        group.changeRequired(request.isRequired());
        group.changeActive(request.isActive());
        validateSelectRange(group.getMinSelect(), group.getMaxSelect());   // 최종 값으로 검증(실패 시 트랜잭션 롤백)

        return MenuOptionGroupResponse.from(group);
    }

    @Transactional
    public LocalDateTime deleteOptionGroup(UUID id, Long userId) {
        MenuOptionGroup group = optionGroupRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.OPTION_GROUP_NOT_FOUND));

        if (group.isDeleted()) {
            throw new CustomException(ErrorCode.ALREADY_DELETED_OPTION_GROUP);
        }
        // TODO(권한, auth 브랜치): 소유권 검증
        optionRepository.findAllByOptionGroupIdAndDeletedAtIsNull(id)
                .forEach(option -> option.softDelete(userId));
        group.softDelete(userId);
        return group.getDeletedAt();
    }

    private MenuOptionGroup findActiveGroup(UUID id) {
        return optionGroupRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new CustomException(ErrorCode.OPTION_GROUP_NOT_FOUND));
    }

    private void validateSelectRange(int minSelect, int maxSelect) {
        if (minSelect > maxSelect) {
            throw new CustomException(ErrorCode.INVALID_OPTION_SELECT_RANGE);
        }
    }
}
