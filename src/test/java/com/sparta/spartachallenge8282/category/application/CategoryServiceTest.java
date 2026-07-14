package com.sparta.spartachallenge8282.category.application;

import com.sparta.spartachallenge8282.category.domain.Category;
import com.sparta.spartachallenge8282.category.domain.CategoryRepository;
import com.sparta.spartachallenge8282.category.presentation.dto.request.CategoryCreateRequest;
import com.sparta.spartachallenge8282.category.presentation.dto.request.CategoryUpdateRequest;
import com.sparta.spartachallenge8282.category.presentation.dto.response.CategoryCreateResponse;
import com.sparta.spartachallenge8282.category.presentation.dto.response.CategoryDeleteResponse;
import com.sparta.spartachallenge8282.category.presentation.dto.response.CategoryResponse;
import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.store.domain.StoreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * CategoryService 단위 테스트 — Repository 를 {@code @Mock} 으로 대체해 서비스 로직만 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private CategoryService categoryService;

    // ── 생성 ────────────────────────────────────────────────────────────────

    @Test
    void 카테고리생성_성공하면_생성된_id를_반환한다() {
        // given
        CategoryCreateRequest request = new CategoryCreateRequest("한식", 1, true);
        given(categoryRepository.existsByNameAndDeletedAtIsNull("한식")).willReturn(false);

        UUID generatedId = UUID.randomUUID();
        Category saved = Category.builder()
                .name("한식").sortOrder(1).isActive(true)
                .build();
        ReflectionTestUtils.setField(saved, "id", generatedId);   // 빌더로는 id 를 못 넣어서 주입
        given(categoryRepository.save(any(Category.class))).willReturn(saved);

        // when
        CategoryCreateResponse result = categoryService.createCategory(request);

        // then
        assertThat(result.categoryId()).isEqualTo(generatedId);
    }

    @Test
    void 카테고리생성_이름중복이면_DUPLICATE_CATEGORY_NAME() {
        // given
        CategoryCreateRequest request = new CategoryCreateRequest("한식", 1, true);
        given(categoryRepository.existsByNameAndDeletedAtIsNull("한식")).willReturn(true);

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> categoryService.createCategory(request));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_CATEGORY_NAME);
        verify(categoryRepository, never()).save(any());
    }

    // ── 단건 조회 ──────────────────────────────────────────────────────────────

    @Test
    void 단건조회_성공하면_CategoryResponse를_반환한다() {
        // given
        UUID id = UUID.randomUUID();
        Category category = Category.builder()
                .name("한식").sortOrder(1).isActive(true)
                .build();
        ReflectionTestUtils.setField(category, "id", id);
        given(categoryRepository.findByIdAndDeletedAtIsNullAndIsActiveTrue(id)).willReturn(Optional.of(category));

        // when
        CategoryResponse result = categoryService.getCategory(id);

        // then
        assertThat(result.categoryId()).isEqualTo(id);
        assertThat(result.name()).isEqualTo("한식");
        assertThat(result.sortOrder()).isEqualTo(1);
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void 단건조회_없는id는_CATEGORY_NOT_FOUND를_던진다() {
        // given
        UUID id = UUID.randomUUID();
        given(categoryRepository.findByIdAndDeletedAtIsNullAndIsActiveTrue(id)).willReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> categoryService.getCategory(id));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
    }

    // ── 목록 조회 ──────────────────────────────────────────────────────────────

    @Test
    void 목록조회_성공하면_페이징된_CategoryResponse를_반환한다() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        UUID id = UUID.randomUUID();
        Category category = Category.builder()
                .name("한식").sortOrder(1).isActive(true)
                .build();
        ReflectionTestUtils.setField(category, "id", id);
        Page<Category> page = new PageImpl<>(List.of(category), pageable, 1);

        given(categoryRepository.searchCategories("한식", true, pageable)).willReturn(page);

        // when
        Page<CategoryResponse> result = categoryService.getCategoryList("한식", pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).categoryId()).isEqualTo(id);
        assertThat(result.getContent().get(0).name()).isEqualTo("한식");
    }

    @Test
    void 목록조회_keyword가_null이면_빈문자열로_검색한다() {
        // given
        Pageable pageable = PageRequest.of(0, 3);
        Pageable normalized = PageRequest.of(0, 10);
        Page<Category> page = new PageImpl<>(List.of(), normalized, 0);

        given(categoryRepository.searchCategories("", true, normalized)).willReturn(page);

        // when
        categoryService.getCategoryList(null, pageable);

        // then
        verify(categoryRepository).searchCategories("", true, normalized);
    }

    // ── 수정 ────────────────────────────────────────────────────────────────

    @Test
    void 카테고리수정_성공하면_수정된_CategoryResponse를_반환한다() {
        // given
        UUID id = UUID.randomUUID();
        Category category = Category.builder()
                .name("한식").sortOrder(1).isActive(true)
                .build();
        ReflectionTestUtils.setField(category, "id", id);
        CategoryUpdateRequest request = new CategoryUpdateRequest("분식", 2, false);

        given(categoryRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.of(category));
        given(categoryRepository.existsByNameAndDeletedAtIsNull("분식")).willReturn(false);

        // when
        CategoryResponse result = categoryService.updateCategory(id, request);

        // then
        assertThat(result.categoryId()).isEqualTo(id);
        assertThat(result.name()).isEqualTo("분식");
        assertThat(result.sortOrder()).isEqualTo(2);
        assertThat(result.isActive()).isFalse();
    }

    @Test
    void 카테고리수정_없는id는_CATEGORY_NOT_FOUND를_던진다() {
        // given
        UUID id = UUID.randomUUID();
        CategoryUpdateRequest request = new CategoryUpdateRequest("분식", 2, false);

        given(categoryRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> categoryService.updateCategory(id, request));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
        verify(categoryRepository, never()).existsByNameAndDeletedAtIsNull(any());
    }

    @Test
    void 카테고리수정_이름중복이면_DUPLICATE_CATEGORY_NAME을_던진다() {
        // given
        UUID id = UUID.randomUUID();
        Category category = Category.builder()
                .name("한식").sortOrder(1).isActive(true)
                .build();
        ReflectionTestUtils.setField(category, "id", id);
        CategoryUpdateRequest request = new CategoryUpdateRequest("분식", null, null);

        given(categoryRepository.findByIdAndDeletedAtIsNull(id)).willReturn(Optional.of(category));
        given(categoryRepository.existsByNameAndDeletedAtIsNull("분식")).willReturn(true);

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> categoryService.updateCategory(id, request));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_CATEGORY_NAME);
    }

    // ── 삭제 ────────────────────────────────────────────────────────────────

    @Test
    void 카테고리삭제_성공하면_deletedAt을_반환하고_소프트삭제된다() {
        // given
        UUID id = UUID.randomUUID();
        Long userId = 1L;
        Category category = Category.builder()
                .name("한식").sortOrder(1).isActive(true)
                .build();
        ReflectionTestUtils.setField(category, "id", id);

        given(categoryRepository.findById(id)).willReturn(Optional.of(category));
        given(storeRepository.existsByCategory_IdAndDeletedAtIsNull(id)).willReturn(false);

        // when
        CategoryDeleteResponse result = categoryService.deleteCategory(id, userId);

        // then
        assertThat(result.deletedAt()).isNotNull();
        assertThat(category.isDeleted()).isTrue();
        assertThat(category.getDeletedAt()).isEqualTo(result.deletedAt());
        assertThat(result.categoryId()).isEqualTo(id);
        assertThat(category.getDeletedBy()).isEqualTo(userId);
    }

    @Test
    void 카테고리삭제_사용중인_카테고리면_CATEGORY_IN_USE를_던진다() {
        // given
        UUID id = UUID.randomUUID();
        Category category = Category.builder()
                .name("한식").sortOrder(1).isActive(true)
                .build();
        ReflectionTestUtils.setField(category, "id", id);

        given(categoryRepository.findById(id)).willReturn(Optional.of(category));
        given(storeRepository.existsByCategory_IdAndDeletedAtIsNull(id)).willReturn(true);

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> categoryService.deleteCategory(id, 1L));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CATEGORY_IN_USE);
        assertThat(category.isDeleted()).isFalse();
    }

    @Test
    void 카테고리삭제_없는id는_CATEGORY_NOT_FOUND를_던진다() {
        // given
        UUID id = UUID.randomUUID();
        given(categoryRepository.findById(id)).willReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> categoryService.deleteCategory(id, 1L));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
    }

    @Test
    void 카테고리삭제_이미삭제된_카테고리면_ALREADY_DELETED_CATEGORY를_던진다() {
        // given
        UUID id = UUID.randomUUID();
        Category category = Category.builder()
                .name("한식").sortOrder(1).isActive(true)
                .build();
        ReflectionTestUtils.setField(category, "id", id);
        ReflectionTestUtils.setField(category, "deletedAt", LocalDateTime.now());

        given(categoryRepository.findById(id)).willReturn(Optional.of(category));

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> categoryService.deleteCategory(id, 1L));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ALREADY_DELETED_CATEGORY);
    }
}
