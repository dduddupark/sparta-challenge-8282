package com.sparta.spartachallenge8282.review.controller;


import com.sparta.spartachallenge8282.global.common.ApiResponse;
import com.sparta.spartachallenge8282.review.dto.*;
import com.sparta.spartachallenge8282.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ReviewController {

    private final ReviewService reviewService;

    // 요청 받아서 dto를 넘겨줌
    @PostMapping("/reviews")
    public ResponseEntity<ApiResponse<ResReviewResultDto>> createReview(
            @RequestParam Long userId, // TODO: JWT 완성되면 @AuthenticationPrincipal로 교체
            @RequestParam UUID storeId,
            @Valid @RequestBody ReqCreateReviewDto dto) {
        ResReviewResultDto response = reviewService.createReview(dto, userId, storeId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("리뷰가 등록되었습니다.",response));
    }

    // 특정 가게의 리뷰 목록 조회 (페이징)
    @GetMapping("/stores/{storeId}/reviews")
    public ResponseEntity<ApiResponse<ResReviewSliceDto>> getReviewsByStore(
            @PathVariable UUID storeId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        ResReviewSliceDto response = reviewService.getReviewsByStore(storeId, pageable);

        return ResponseEntity.ok(ApiResponse.success("조회 성공", response));
    }

    @GetMapping("/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<ResReviewDto>> getReview(
            @PathVariable UUID reviewId) {

        ResReviewDto response = reviewService.getReview(reviewId);

        return ResponseEntity.ok(ApiResponse.success("조회 성공", response));
    }

    @PatchMapping("/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<ResReviewResultDto>> updateReview(
            @PathVariable UUID reviewId,
            @RequestParam Long userId,
            @Valid @RequestBody ReqUpdateReviewDto requestDto) {

        ResReviewResultDto response = reviewService.updateReview(reviewId, userId, requestDto);

        return ResponseEntity.ok(ApiResponse.success("리뷰가 수정되었습니다.", response));
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable UUID reviewId,
            @RequestParam Long userId) { // TODO: JWT 완성되면 @AuthenticationPrincipal로 교체

        reviewService.deleteReview(reviewId, userId);

        return ResponseEntity.ok(ApiResponse.success("리뷰가 삭제되었습니다."));
    }
}
