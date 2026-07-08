package com.sparta.spartachallenge8282.review.dto.response;

import java.util.UUID;
/**
 * 리뷰 응답 DTO
 * 리뷰 응답시 리뷰 아이디를 반환 -> API 응답 확인용
 * */

public record ReviewResultResponseDto(
       UUID reviewId
) {
    public static ReviewResultResponseDto from(UUID reviewId) {
        return new ReviewResultResponseDto(reviewId);
    }
}
