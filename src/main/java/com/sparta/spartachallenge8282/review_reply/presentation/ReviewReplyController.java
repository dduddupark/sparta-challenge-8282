package com.sparta.spartachallenge8282.review_reply.presentation;


import com.sparta.spartachallenge8282.global.common.ApiResponse;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import com.sparta.spartachallenge8282.global.common.PageableUtil;
import com.sparta.spartachallenge8282.review_reply.presentation.dto.request.ReviewReplyRequestDto;
import com.sparta.spartachallenge8282.review_reply.presentation.dto.response.ReviewReplyResponseDto;
import com.sparta.spartachallenge8282.review_reply.presentation.dto.response.ReviewReplySliceResponseDto;
import com.sparta.spartachallenge8282.review_reply.application.ReviewReplyService;
import com.sparta.spartachallenge8282.user.domain.UserRole;
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
 * 답글(ReviewReply) API.
 *
 * 답글은 리뷰와 1:1 관계라 URL에 replyId가 필요 없다 — reviewId 하나로 특정 가능하다.
 * 그래서 경로가 /reviews/{reviewId}/reply(단수)로 되어있다.
 * 답글 단독 조회 API는 없으며, 리뷰 상세/목록 조회 응답의 reply 필드로 함께 내려간다.
 *
 * 작성/수정/삭제 전부 OWNER 인증이 필요하다(@AuthenticationPrincipal).
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ReviewReplyController {

    private final ReviewReplyService reviewReplyService;

    // 답글 작성
    @PostMapping("/reviews/{reviewId}/reply")
    public ResponseEntity<ApiResponse<ReviewReplyResponseDto>> createReply(
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody ReviewReplyRequestDto requestDto) {

        ReviewReplyResponseDto response = reviewReplyService.createReply(requestDto, reviewId, userDetails.userId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("답글이 등록되었습니다.",response));
    }

    // 답글 수정
    @PatchMapping("/reviews/{reviewId}/reply")
    public ResponseEntity<ApiResponse<ReviewReplyResponseDto>> updateReply(
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody ReviewReplyRequestDto requestDto) {

        ReviewReplyResponseDto response = reviewReplyService.updateReply(requestDto, reviewId, userDetails.userId());

        return ResponseEntity.ok(ApiResponse.success("답글이 수정되었습니다.", response));
    }

    // 답글 삭제
    @DeleteMapping("/reviews/{reviewId}/reply")
    public ResponseEntity<ApiResponse<Void>> deleteReply(
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
            ){

        UserRole role = UserRole.valueOf(userDetails.role().substring(5));
        reviewReplyService.deleteReply(reviewId, userDetails.userId(), role);

        return ResponseEntity.ok(ApiResponse.success("답글이 삭제되었습니다."));
    }

    // 특정 가게의 답글 목록 조회 (페이징)
    @GetMapping("/replies")
    public ResponseEntity<ApiResponse<ReviewReplySliceResponseDto>> getRepliesByStore(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam UUID storeId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Pageable normalized = PageableUtil.normalize(pageable);
        ReviewReplySliceResponseDto response = reviewReplyService.getRepliesByStore(storeId, normalized);
        return ResponseEntity.ok(ApiResponse.success("조회 성공", response));
    }

}
