package com.sparta.spartachallenge8282.category.presentation.dto.response;

import com.sparta.spartachallenge8282.category.domain.Category;

import java.util.UUID;

/**
 * 카테고리 생성 응답. (POST /api/v1/categories → 201 Created)
 */
public record CategoryCreateResponse(UUID categoryId) {
    public static CategoryCreateResponse from(Category category) {
        return new CategoryCreateResponse(category.getId());
    }
}
