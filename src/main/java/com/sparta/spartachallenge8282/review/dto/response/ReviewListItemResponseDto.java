package com.sparta.spartachallenge8282.review.dto.response;


import com.sparta.spartachallenge8282.review.entity.Review;

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
        Object reply, // 임시 답글 객체
        LocalDateTime createdAt
) {



    public static ReviewListItemResponseDto from(Review review) {
        return new ReviewListItemResponseDto(
                review.getId(),
                null,
                review.getRating(),
                review.getContent(),
                review.getImageUrl(),
                null,
                review.getCreatedAt()
        );

    }

}
