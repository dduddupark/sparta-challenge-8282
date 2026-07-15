package com.sparta.spartachallenge8282.menu.presentation.dto.response;

import com.sparta.spartachallenge8282.menu.domain.PreviewMenuProjection;

import java.util.UUID;

public record StorePreviewMenuResponse(
        UUID menuId,
        String name,
        Integer sortOrder,
        Integer price
) {
    public static StorePreviewMenuResponse from(
            PreviewMenuProjection projection
    ) {
        return new StorePreviewMenuResponse(
                projection.getMenuId(),
                projection.getName(),
                projection.getSortOrder(),
                projection.getPrice()

        );
    }
}
