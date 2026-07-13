package com.sparta.spartachallenge8282.menu.option.presentation.dto.response;

import com.sparta.spartachallenge8282.menu.option.domain.MenuOption;

import java.util.UUID;

public record MenuOptionResponse(
        UUID optionId,
        UUID optionGroupId,
        String name,
        int additionalPrice,
        int sortOrder,
        boolean isActive
) {
    public static MenuOptionResponse from(MenuOption option) {
        return new MenuOptionResponse(
                option.getId(),
                option.getOptionGroupId(),
                option.getName(),
                option.getAdditionalPrice(),
                option.getSortOrder(),
                option.isActive()
        );
    }
}
