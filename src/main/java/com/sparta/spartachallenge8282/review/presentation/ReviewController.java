package com.sparta.spartachallenge8282.review.presentation;


import com.sparta.spartachallenge8282.global.common.ApiResponse;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import com.sparta.spartachallenge8282.review.application.ReviewService;
import com.sparta.spartachallenge8282.review.presentation.dto.request.ReviewCreateRequestDto;
import com.sparta.spartachallenge8282.review.presentation.dto.request.ReviewUpdateRequestDto;
import com.sparta.spartachallenge8282.review.presentation.dto.response.ReviewResponseDto;
import com.sparta.spartachallenge8282.review.presentation.dto.response.ReviewResultResponseDto;
import com.sparta.spartachallenge8282.review.presentation.dto.response.ReviewSliceResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 리뷰(Review) API.
 * 생성/수정/삭제는 @AuthenticationPrincipal로 로그인한 본인의 userId를 받는다.
 * 목록 조회(GET /stores/{storeId}/reviews)와 상세 조회(GET /reviews/{reviewId})는 인증 없이 누구나 접근 가능하다.
 * 목록 조회는 Slice 기반 페이징이며 기본값은 size=10, createdAt 내림차순이다.
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ReviewController {

    private final ReviewService reviewService;

    // 리뷰 작성
    @PostMapping("/reviews")
    public ResponseEntity<ApiResponse<ReviewResultResponseDto>> createReview(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody ReviewCreateRequestDto requestDto) {

        Long userId = userDetails.userId();

        ReviewResultResponseDto response = reviewService.createReview(requestDto, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("리뷰가 등록되었습니다.",response));
    }

    // 특정 가게의 리뷰 목록 조회 (페이징)
    @GetMapping("/stores/{storeId}/reviews")
    public ResponseEntity<ApiResponse<ReviewSliceResponseDto>> getReviewsByStore(
            @PathVariable UUID storeId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        ReviewSliceResponseDto response = reviewService.getReviewsByStore(storeId, pageable);

        return ResponseEntity.ok(ApiResponse.success("조회 성공", response));
    }

    // 리뷰 상세보기
    @GetMapping("/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewResponseDto>> getReview(
            @PathVariable UUID reviewId) {

        ReviewResponseDto response = reviewService.getReview(reviewId);

        return ResponseEntity.ok(ApiResponse.success("조회 성공", response));
    }

    // 리뷰 수정하기
    @PatchMapping("/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewResultResponseDto>> updateReview(
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody ReviewUpdateRequestDto requestDto) {

        ReviewResultResponseDto response = reviewService.updateReview(reviewId, userDetails.userId(), requestDto);

        return ResponseEntity.ok(ApiResponse.success("리뷰가 수정되었습니다.", response));
    }

    // 리뷰 삭제하기
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {

        reviewService.deleteReview(reviewId, userDetails.userId(), userDetails.role());

        return ResponseEntity.ok(ApiResponse.success("리뷰가 삭제되었습니다."));
    }
}
