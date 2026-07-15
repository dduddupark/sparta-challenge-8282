package com.sparta.spartachallenge8282.option.presentation.dto.response;

import com.sparta.spartachallenge8282.option.domain.MenuOption;

import java.util.UUID;

/**
 * 옵션 생성 응답. (POST /api/v1/option-groups/{optionGroupId}/options → 201 Created)
 */
public record MenuOptionCreateResponse(UUID optionId) {
    public static MenuOptionCreateResponse from(MenuOption option) {
        return new MenuOptionCreateResponse(option.getId());
    }
}
