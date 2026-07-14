package com.sparta.spartachallenge8282.category.presentation;

import com.sparta.spartachallenge8282.category.application.CategoryService;
import com.sparta.spartachallenge8282.category.presentation.dto.request.CategoryCreateRequest;
import com.sparta.spartachallenge8282.category.presentation.dto.request.CategoryUpdateRequest;
import com.sparta.spartachallenge8282.category.presentation.dto.response.CategoryCreateResponse;
import com.sparta.spartachallenge8282.category.presentation.dto.response.CategoryDeleteResponse;
import com.sparta.spartachallenge8282.category.presentation.dto.response.CategoryResponse;
import com.sparta.spartachallenge8282.global.common.ApiResponse;
import com.sparta.spartachallenge8282.global.common.PageResponse;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 카테고리 REST 컨트롤러.
 *
 * <p>카테고리는 특정 가게에 종속되지 않는 플랫폼 공통 마스터 데이터다.
 * 쓰기 요청은 MANAGER/MASTER 권한이 필요하며, 조회는 비로그인 공개다.
 */
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_MASTER')")
    @PostMapping
    public ResponseEntity<ApiResponse<CategoryCreateResponse>> createCategory(
            @Valid @RequestBody CategoryCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("카테고리 생성 완료", categoryService.createCategory(request)));
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategory(@PathVariable UUID categoryId) {
        return ResponseEntity.ok(
                ApiResponse.success("카테고리 조회 성공", categoryService.getCategory(categoryId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CategoryResponse>>> getCategoryList(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<CategoryResponse> data =
                PageResponse.from(categoryService.getCategoryList(keyword, pageable));
        return ResponseEntity.ok(ApiResponse.success("카테고리 목록 조회 성공", data));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_MASTER')")
    @PatchMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable UUID categoryId,
            @Valid @RequestBody CategoryUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("카테고리 수정 완료", categoryService.updateCategory(categoryId, request)));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_MASTER')")
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<CategoryDeleteResponse>> deleteCategory(
            @PathVariable UUID categoryId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(
                ApiResponse.success("카테고리 삭제 완료", categoryService.deleteCategory(categoryId, userDetails.userId())));
    }
}
