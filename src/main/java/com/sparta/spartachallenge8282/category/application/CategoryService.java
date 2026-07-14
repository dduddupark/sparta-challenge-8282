package com.sparta.spartachallenge8282.category.application;

import com.sparta.spartachallenge8282.category.domain.Category;
import com.sparta.spartachallenge8282.category.domain.CategoryRepository;
import com.sparta.spartachallenge8282.category.presentation.dto.request.CategoryCreateRequest;
import com.sparta.spartachallenge8282.category.presentation.dto.request.CategoryUpdateRequest;
import com.sparta.spartachallenge8282.category.presentation.dto.response.CategoryCreateResponse;
import com.sparta.spartachallenge8282.category.presentation.dto.response.CategoryDeleteResponse;
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

    /**
     * 카테고리 생성.
     *
     * <p>이름 중복은 여기서 먼저 걸러 {@code DUPLICATE_CATEGORY_NAME} 로 응답하고,
     * 동시 요청의 최종 방어는 partial unique index({@code uk_category_name_active})가 담당한다.
     */
    @Transactional
    public CategoryCreateResponse createCategory(CategoryCreateRequest request) {
        if(categoryRepository.existsByNameAndDeletedAtIsNull(request.name())) {
            throw new CustomException((ErrorCode.DUPLICATE_CATEGORY_NAME));
        }

        Category category = Category.builder()
                .name(request.name())
                .sortOrder(request.sortOrder())
                .isActive(request.isActive())
                .build();

        return CategoryCreateResponse.from(categoryRepository.save(category));
    }

    /** 카테고리 단건 조회. (비활성 카테고리는 조회 대상에서 제외한다 — {@code CATEGORY_NOT_FOUND}) */
    public CategoryResponse getCategory(UUID id) {
        Category category = categoryRepository.findByIdAndDeletedAtIsNullAndIsActiveTrue(id)
                .orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND));
        return CategoryResponse.from(category);
    }

    /** 카테고리 목록 검색. (공개 — 활성 항목만, 페이지 크기는 10/30/50 만 허용) */
    public Page<CategoryResponse> getCategoryList(String keyword, Pageable pageable) {
        String searchKeyword = (keyword == null) ? "" : keyword;   // keyword 없으면 LIKE '%%'로 전체 조회
        Pageable normalizedPageable = PageableUtil.normalize(pageable);

        // 공개 조회는 활성 항목만 노출한다.
        // TODO(관리자 확장): 비활성 포함 전체 조회는 admin 엔드포인트에서 searchCategories에 isActive를 전달해 재사용한다.
        return categoryRepository.searchCategories(searchKeyword, true, normalizedPageable)
                .map(CategoryResponse::from);
    }

    /**
     * 카테고리 수정 (부분 수정 — null 인 필드는 변경하지 않는다).
     *
     * <p>이름 중복 검사는 "이름을 실제로 바꾸는 경우"에만 수행한다 — 수정 폼이 기존 이름을 그대로 돌려보내도
     * 자기 자신과 중복이라며 거부되지 않도록.
     *
     * <p>수정은 비활성 카테고리도 대상이다(다시 활성화할 수 있어야 하므로).
     */
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

    /**
     * 카테고리 삭제 (소프트 삭제).
     *
     * <p>사용 중인 가게가 있으면 {@code CATEGORY_IN_USE} 로 거부한다 — 참조 무결성은 FK 가 아니라
     * 애플리케이션이 지킨다(소프트 삭제라 DB FK 로는 걸러지지 않는다).
     *
     * @param userId 삭제를 수행한 사용자 ID ({@code deleted_by} 에 기록)
     */
    @Transactional
    public CategoryDeleteResponse deleteCategory(UUID id, Long userId) {
        Category category = categoryRepository.findById(id)   // 이미 삭제된 것과 없는 것을 구분하려 삭제 포함 조회
                .orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND));

        if (category.isDeleted()) {
            throw new CustomException(ErrorCode.ALREADY_DELETED_CATEGORY);
        }

        if (storeRepository.existsByCategory_IdAndDeletedAtIsNull(id)) {
            throw new CustomException(ErrorCode.CATEGORY_IN_USE);
        }

        category.softDelete(userId);
        return CategoryDeleteResponse.from(category);
    }
}
