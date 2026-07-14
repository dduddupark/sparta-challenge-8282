package com.sparta.spartachallenge8282.review.presentation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 리뷰 수정 요청 DTO.
 * 모든 필드가 선택(optional)이다 — orderId 는 아예 포함하지 않으며
 * (수정 시 주문을 바꿀 수 없음), 보내지 않은 필드는 Review.update() 에서
 * 기존 값을 그대로 유지한다. rating 은 값이 오면 1~5 범위만 검증하고(@Min/@Max)
 * 생성 요청과 달리 @NotNull 은 붙이지 않는다.
 */

public record ReviewUpdateRequestDto(
        @Min(value = 1, message = "평점은 1점 이상이어야 합니다.")
        @Max(value = 5, message = "평점은 5점 이하여야 합니다.")
        Integer rating,   // null 가능 (@NotNull 없음)
        String content,   // null 가능
        String imageUrl  // null 가능

) {

}
