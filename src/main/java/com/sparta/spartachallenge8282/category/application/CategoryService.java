package com.sparta.spartachallenge8282.category.application;

import com.sparta.spartachallenge8282.category.domain.CategoryRepository;
import com.sparta.spartachallenge8282.category.presentation.dto.request.CategoryCreateRequest;
import com.sparta.spartachallenge8282.category.presentation.dto.request.CategoryUpdateRequest;
import com.sparta.spartachallenge8282.category.presentation.dto.response.CategoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 카테고리 비즈니스 로직.
 *
 * <p>조회는 클래스 기본 {@code @Transactional(readOnly = true)}, 쓰기 메서드만 {@code @Transactional} 로 오버라이드한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public UUID createCategory(CategoryCreateRequest request) {
        throw new UnsupportedOperationException("TODO: createCategory 구현");
    }

    public CategoryResponse getCategory(UUID id) {
        throw new UnsupportedOperationException("TODO: getCategory 구현");
    }

    public Page<CategoryResponse> getCategoryList(String keyword, Boolean isActive, Pageable pageable) {
        throw new UnsupportedOperationException("TODO: getCategoryList 구현");
    }

    @Transactional
    public CategoryResponse updateCategory(UUID id, CategoryUpdateRequest request) {
        throw new UnsupportedOperationException("TODO: updateCategory 구현");
    }

    @Transactional
    public LocalDateTime deleteCategory(UUID id, Long userId) {
        throw new UnsupportedOperationException("TODO: deleteCategory 구현");
    }
}
