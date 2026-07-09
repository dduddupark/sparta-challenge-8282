package com.sparta.spartachallenge8282.review_reply.controller;


import com.sparta.spartachallenge8282.global.common.ApiResponse;
import com.sparta.spartachallenge8282.review.dto.response.ReviewResultResponseDto;
import com.sparta.spartachallenge8282.review_reply.dto.ReviewReplyRequestDto;
import com.sparta.spartachallenge8282.review_reply.dto.ReviewReplyResponseDto;
import com.sparta.spartachallenge8282.review_reply.service.ReviewReplyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ReviewReplyController {

    private final ReviewReplyService reviewReplyService;

    // 답글 작성
    @PostMapping("/reviews/{reviewId}/reply")
    public ResponseEntity<ApiResponse<ReviewReplyResponseDto>> createReply(
            @PathVariable UUID reviewId,
            @RequestParam UUID storeId,
            @RequestParam  Long userId,
            @Valid @RequestBody ReviewReplyRequestDto requestDto) {

        ReviewReplyResponseDto response = reviewReplyService.createReply(requestDto, reviewId, userId, storeId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("답글이 등록되었습니다.",response));
    }

    // 답글 수정
    @PatchMapping("/reviews/{reviewId}/reply")
    public ResponseEntity<ApiResponse<ReviewReplyResponseDto>> updateReply(
            @PathVariable UUID reviewId,
            @RequestParam UUID storeId,
            @RequestParam  Long userId,
            @Valid @RequestBody ReviewReplyRequestDto requestDto) {

        ReviewReplyResponseDto response = reviewReplyService.updateReply(requestDto, reviewId, userId, storeId);

        return ResponseEntity.ok(ApiResponse.success("답글이 수정되었습니다.", response));
    }

    // 답글 삭제
    @DeleteMapping("/reviews/{reviewId}/reply")
    public ResponseEntity<ApiResponse<Void>> deleteReply(
            @PathVariable UUID reviewId,
            @RequestParam UUID storeId,
            @RequestParam Long userId // TODO: User 완성 후 @AuthenticationPrincipal로 교체
            ){   // TODO: 임시

        reviewReplyService.deleteReply(reviewId, userId, storeId);

        return ResponseEntity.ok(ApiResponse.success("답글이 삭제되었습니다."));
    }

}
