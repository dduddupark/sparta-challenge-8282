package com.sparta.spartachallenge8282.review.presentation.dto.response;

import com.sparta.spartachallenge8282.review.domain.Review;
import com.sparta.spartachallenge8282.review_reply.presentation.dto.response.ReviewReplyResponseDto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 리뷰 상세 조회 응답 DTO.
 * 목록 조회 항목과 달리 storeId를 포함한다 - 목록 조회는 URL에
 * storeId가 이미 있어 중복이지만, 상세 조회는 reviewId 하나만으로
 * 접근하므로 응답에 storeId를 명시해야 한다.
 */

public record ReviewResponseDto(
        UUID reviewId,
        UUID storeId,
        String userNickname,
        Integer rating,
        String content,
        String imageUrl,
        LocalDateTime createdAt,
        ReviewReplyResponseDto reply // null 가능
) {
    public static ReviewResponseDto from(Review review, String userNickname, ReviewReplyResponseDto reply) {
        return new ReviewResponseDto(
                review.getId(),
                review.getStoreId(),
                userNickname,
                review.getRating(),
                review.getContent(),
                review.getImageUrl(),
                review.getCreatedAt(),
                reply
        );
    }
}
