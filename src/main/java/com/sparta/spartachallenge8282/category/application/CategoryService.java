package com.sparta.spartachallenge8282.category.application;

import com.sparta.spartachallenge8282.category.domain.Category;
import com.sparta.spartachallenge8282.category.domain.CategoryRepository;
import com.sparta.spartachallenge8282.category.presentation.dto.request.CategoryCreateRequest;
import com.sparta.spartachallenge8282.category.presentation.dto.request.CategoryUpdateRequest;
import com.sparta.spartachallenge8282.category.presentation.dto.response.CategoryResponse;
import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
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
        if(categoryRepository.existsByNameAndDeletedAtIsNull(request.name())) {
            throw new CustomException((ErrorCode.DUPLICATE_CATEGORY_NAME));
        }

        Category category = Category.builder()
                .name(request.name())
                .sortOrder(request.sortOrder())
                .isActive(request.isActive())
                .build();
        Category saved = categoryRepository.save(category);

        return  saved.getId();
    }

    public CategoryResponse getCategory(UUID id) {
        Category category = categoryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND));
        return CategoryResponse.from(category);
    }

    public Page<CategoryResponse> getCategoryList(String keyword, Boolean isActive, Pageable pageable) {
        String searchKeyword = (keyword == null) ? "" : keyword;   // keyword 없으면 LIKE '%%'로 전체 조회

        return categoryRepository.searchCategories(searchKeyword, isActive, pageable)
                .map(CategoryResponse::from);
    }

    @Transactional
    public CategoryResponse updateCategory(UUID id, CategoryUpdateRequest request) {
        Category category = categoryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND));

        if (request.name() != null
                && !request.name().equals(category.getName())
                && categoryRepository.existsByNameAndDeletedAtIsNull(request.name())) {
            throw new CustomException(ErrorCode.DUPLICATE_CATEGORY_NAME);
        }

        category.updateName(request.name());
        category.changeSortOrder(request.sortOrder());
        category.changeActive(request.isActive());

        return CategoryResponse.from(category);
    }

    @Transactional
    public LocalDateTime deleteCategory(UUID id, Long userId) {
        Category category = categoryRepository.findById(id)   // 이미 삭제된 것과 없는 것을 구분하려 삭제 포함 조회
                .orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND));

        if (category.isDeleted()) {
            throw new CustomException(ErrorCode.ALREADY_DELETED_CATEGORY);
        }

        // TODO: CATEGORY_IN_USE — 이 category 를 참조하는 store 가 있으면 예외 (StoreRepository 필요, Store 머지 후)

        category.softDelete(userId);
        return category.getDeletedAt();
    }
}
