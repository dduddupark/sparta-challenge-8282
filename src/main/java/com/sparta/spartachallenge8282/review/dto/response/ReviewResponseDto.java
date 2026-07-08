package com.sparta.spartachallenge8282.review.dto.response;

import com.sparta.spartachallenge8282.review.entity.Review;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 리뷰 데이터 응답 DTO
 * 리뷰에 필요한 데이터
 * */

public record ReviewResponseDto(

        UUID reviewId,
        UUID storeId,
        Integer rating,
        String content,
        String imageUrl,
        LocalDateTime createdAt
) {

    public static ReviewResponseDto from(Review review) {
        return new ReviewResponseDto(
                review.getId(),
                review.getStoreId(),
                review.getRating(),
                review.getContent(),
                review.getImageUrl(),
                review.getCreatedAt()
        );
    }
}
