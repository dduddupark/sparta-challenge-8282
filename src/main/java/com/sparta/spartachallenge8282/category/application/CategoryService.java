package com.sparta.spartachallenge8282.category.application;

import com.sparta.spartachallenge8282.category.domain.Category;
import com.sparta.spartachallenge8282.category.domain.CategoryRepository;
import com.sparta.spartachallenge8282.category.presentation.dto.request.CategoryCreateRequest;
import com.sparta.spartachallenge8282.category.presentation.dto.request.CategoryUpdateRequest;
import com.sparta.spartachallenge8282.category.presentation.dto.response.CategoryResponse;
import com.sparta.spartachallenge8282.global.common.PageableUtil;
import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.store.domain.StoreRepository;
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
 * 조회는 클래스 기본 @Transactional(readOnly = true), 쓰기 메서드만 @Transactional 로 오버라이드한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;

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
        Category category = categoryRepository.findByIdAndDeletedAtIsNullAndIsActiveTrue(id)
                .orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND));
        return CategoryResponse.from(category);
    }

    public Page<CategoryResponse> getCategoryList(String keyword, Pageable pageable) {
        String searchKeyword = (keyword == null) ? "" : keyword;   // keyword 없으면 LIKE '%%'로 전체 조회
        Pageable normalizedPageable = PageableUtil.normalize(pageable);

        // 공개 조회는 활성 항목만 노출한다.
        // TODO(관리자 확장): 비활성 포함 전체 조회는 admin 엔드포인트에서 searchCategories에 isActive를 전달해 재사용한다.
        return categoryRepository.searchCategories(searchKeyword, true, normalizedPageable)
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

        if (storeRepository.existsByCategory_IdAndDeletedAtIsNull(id)) {
            throw new CustomException(ErrorCode.CATEGORY_IN_USE);
        }

        category.softDelete(userId);
        return category.getDeletedAt();
    }
}
