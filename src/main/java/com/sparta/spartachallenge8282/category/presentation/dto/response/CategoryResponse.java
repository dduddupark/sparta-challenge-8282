package com.sparta.spartachallenge8282.category.presentation.dto.response;

import com.sparta.spartachallenge8282.category.domain.Category;

import java.util.UUID;

public record CategoryResponse(
        UUID categoryId,
        String name,
        int sortOrder,
        boolean isActive
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSortOrder(),
                category.isActive()
        );
    }
}
