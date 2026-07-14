package com.sparta.spartachallenge8282.optiongroup.presentation.dto.response;

import com.sparta.spartachallenge8282.optiongroup.domain.MenuOptionGroup;

import java.util.UUID;

public record MenuOptionGroupResponse(
        UUID optionGroupId,
        UUID menuId,
        String name,
        boolean isRequired,
        int minSelect,
        int maxSelect,
        int sortOrder,
        boolean isActive
) {
    public static MenuOptionGroupResponse from(MenuOptionGroup group) {
        return new MenuOptionGroupResponse(
                group.getId(),
                group.getMenuId(),
                group.getName(),
                group.isRequired(),
                group.getMinSelect(),
                group.getMaxSelect(),
                group.getSortOrder(),
                group.isActive()
        );
    }
}
