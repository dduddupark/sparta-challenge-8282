package com.sparta.spartachallenge8282.review.presentation.dto.response;


import com.sparta.spartachallenge8282.review.domain.Review;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 리뷰 리스트 응답 DTO
 * 리뷰 목록을 조회
 * */
public record ReviewListItemResponseDto(
        UUID reviewId,
        String userNickname,
        Integer rating,
        String content,
        String imageUrl,
        LocalDateTime createdAt
) {
    public static ReviewListItemResponseDto from(Review review, String userNickname) {
        return new ReviewListItemResponseDto(
                review.getId(),
                userNickname,
                review.getRating(),
                review.getContent(),
                review.getImageUrl(),
                review.getCreatedAt()
        );
    }
}
