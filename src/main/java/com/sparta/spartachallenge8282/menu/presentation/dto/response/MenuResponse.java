package com.sparta.spartachallenge8282.menu.presentation.dto.response;

import com.sparta.spartachallenge8282.menu.domain.Menu;
import com.sparta.spartachallenge8282.menu.domain.MenuBadge;
import com.sparta.spartachallenge8282.menu.domain.MenuStatus;

import java.util.UUID;

public record MenuResponse(
        UUID menuId,
        UUID storeId,
        String name,
        String description,
        int price,
        int sortOrder,
        MenuStatus status,
        MenuBadge badge,
        boolean isHidden,
        boolean isAiGenerated
) {
    public static MenuResponse from(Menu menu) {
        return new MenuResponse(
                menu.getId(),
                menu.getStoreId(),
                menu.getName(),
                menu.getDescription(),
                menu.getPrice(),
                menu.getSortOrder(),
                menu.getStatus(),
                menu.getBadge(),
                menu.isHidden(),
                menu.isAiGenerated()
        );
    }
}
