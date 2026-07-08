package com.sparta.spartachallenge8282.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 리뷰 수정 요청 DTO
 * 생성과 비슷하지만 평점 기능을 수정안해도 되도록 구현
 * */

public record ReviewUpdateRequestDto(
        @Min(value = 1, message = "평점은 1점 이상이어야 합니다.")
        @Max(value = 5, message = "평점은 5점 이하여야 합니다.")
        Integer rating,   // null 가능 (@NotNull 없음)
        String content,   // null 가능
        String imageUrl  // null 가능

) {

}
