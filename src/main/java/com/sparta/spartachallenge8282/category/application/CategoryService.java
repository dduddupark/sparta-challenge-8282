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
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
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

    private static final String ACTIVE_NAME_UNIQUE_INDEX = "uk_category_name_active";

    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;

    /**
     * 카테고리 생성.
     *
     * <p>Service 의 사전 조회는 일반적인 중복 요청을 이해하기 쉬운 도메인 오류로 빠르게 거절한다.
     * 다만 조회와 INSERT 사이에 다른 요청이 들어오는 경합(TOCTOU)은 막지 못하므로,
     * 활성 행만 대상으로 하는 partial unique index({@code uk_category_name_active})가 최종 정합성을 보장한다.
     * 이 인덱스는 JPA 가 자동 생성하지 않으므로 DB 환경마다 별도로 적용해야 한다.
     */
    @Transactional
    public CategoryCreateResponse createCategory(CategoryCreateRequest request) {
        if (categoryRepository.existsByNameAndDeletedAtIsNull(request.name())) {
            throw new CustomException(ErrorCode.DUPLICATE_CATEGORY_NAME);
        }

        Category category = Category.builder()
                .name(request.name())
                .sortOrder(request.sortOrder())
                .isActive(request.isActive())
                .build();

        try {
            // 커밋까지 미루지 않고 여기서 INSERT 해 DB 유일성 위반을 도메인 오류로 변환한다.
            return CategoryCreateResponse.from(categoryRepository.saveAndFlush(category));
        } catch (DataIntegrityViolationException exception) {
            throw translateActiveNameConflict(exception);
        }
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

        boolean nameChanged = request.name() != null
                && !request.name().isBlank()
                && !request.name().equals(category.getName());

        if (nameChanged && categoryRepository.existsByNameAndDeletedAtIsNull(request.name())) {
            throw new CustomException(ErrorCode.DUPLICATE_CATEGORY_NAME);
        }

        category.updateName(request.name());
        category.changeSortOrder(request.sortOrder());
        category.changeActive(request.isActive());

        if (nameChanged) {
            try {
                // 이름 변경도 동시 요청이 가능하므로 트랜잭션 종료 전에 제약조건을 확인한다.
                categoryRepository.flush();
            } catch (DataIntegrityViolationException exception) {
                throw translateActiveNameConflict(exception);
            }
        }

        return CategoryResponse.from(category);
    }

    /** 해당 partial unique index 충돌만 이름 중복 오류로 바꾸고, 다른 DB 오류는 원인을 보존한다. */
    private RuntimeException translateActiveNameConflict(DataIntegrityViolationException exception) {
        if (isCausedByConstraint(exception, ACTIVE_NAME_UNIQUE_INDEX)) {
            return new CustomException(ErrorCode.DUPLICATE_CATEGORY_NAME);
        }
        return exception;
    }

    private boolean isCausedByConstraint(Throwable throwable, String constraintName) {
        Throwable cause = throwable;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation
                    && constraintName.equalsIgnoreCase(constraintViolation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
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
