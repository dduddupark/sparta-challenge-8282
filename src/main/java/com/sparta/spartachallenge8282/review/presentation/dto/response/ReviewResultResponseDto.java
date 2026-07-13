package com.sparta.spartachallenge8282.review.presentation.dto.response;

import java.util.UUID;

/**
 * 리뷰 생성, 수정 공용 응답 DTO.
 * 명세상 두 API 모두 reviewId 하나만 반환하므로 공용으로 사용한다.
 * 필드가 늘어나 응답 형태가 서로 달라지면 그때 분리한다.
 */

public record ReviewResultResponseDto(
       UUID reviewId
) {
    public static ReviewResultResponseDto from(UUID reviewId) {
        return new ReviewResultResponseDto(reviewId);
    }
}
