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
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PreAuthorize("hasAnyRole('MANAGER','MASTER')")
    @PostMapping
    public ResponseEntity<ApiResponse<CategoryCreateResponse>> createCategory(
            @Valid @RequestBody CategoryCreateRequest request) {
        UUID categoryId = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("카테고리 생성 완료", new CategoryCreateResponse(categoryId)));
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategory(@PathVariable UUID categoryId) {
        return ResponseEntity.ok(
                ApiResponse.success("카테고리 조회 성공", categoryService.getCategory(categoryId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CategoryResponse>>> getCategoryList(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "sortOrder") Pageable pageable) {
        PageResponse<CategoryResponse> data =
                PageResponse.from(categoryService.getCategoryList(keyword, pageable));
        return ResponseEntity.ok(ApiResponse.success("카테고리 목록 조회 성공", data));
    }

    @PreAuthorize("hasAnyRole('MANAGER','MASTER')")
    @PatchMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable UUID categoryId,
            @Valid @RequestBody CategoryUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("카테고리 수정 완료", categoryService.updateCategory(categoryId, request)));
    }

    @PreAuthorize("hasAnyRole('MANAGER','MASTER')")
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<CategoryDeleteResponse>> deleteCategory(
            @PathVariable UUID categoryId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        LocalDateTime deletedAt = categoryService.deleteCategory(categoryId, userDetails.userId());
        return ResponseEntity.ok(
                ApiResponse.success("카테고리 삭제 완료", new CategoryDeleteResponse(categoryId, deletedAt)));
    }
}
