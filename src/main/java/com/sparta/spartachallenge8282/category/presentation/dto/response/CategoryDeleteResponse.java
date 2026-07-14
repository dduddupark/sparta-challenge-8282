package com.sparta.spartachallenge8282.category.presentation.dto.response;

import com.sparta.spartachallenge8282.category.domain.Category;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 카테고리 삭제 응답. (DELETE /api/v1/categories/{categoryId})
 *
 * <p>소프트 삭제이므로 삭제 시각({@code deletedAt})을 함께 반환한다.
 */
public record CategoryDeleteResponse(UUID categoryId, LocalDateTime deletedAt) {
    public static CategoryDeleteResponse from(Category category) {
        return new CategoryDeleteResponse(category.getId(), category.getDeletedAt());
    }
}
