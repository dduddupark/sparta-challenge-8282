package com.sparta.spartachallenge8282.optiongroup.presentation.dto.response;

import com.sparta.spartachallenge8282.optiongroup.domain.MenuOptionGroup;

import java.util.UUID;

/**
 * 옵션 그룹 생성 응답. (POST /api/v1/menus/{menuId}/option-groups → 201 Created)
 */
public record MenuOptionGroupCreateResponse(UUID optionGroupId) {
    public static MenuOptionGroupCreateResponse from(MenuOptionGroup optionGroup) {
        return new MenuOptionGroupCreateResponse(optionGroup.getId());
    }
}
