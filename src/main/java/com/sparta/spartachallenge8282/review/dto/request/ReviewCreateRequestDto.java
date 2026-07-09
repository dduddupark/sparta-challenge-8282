package com.sparta.spartachallenge8282.review.dto.request;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
* 리뷰 생성 요청 DTO
* 주문과 평점을 Not Null로 받음.
* */

public record ReviewCreateRequestDto(
        @NotNull(message = "주문 ID는 필수입니다.")
        UUID orderId,

        @NotNull(message = "평점은 필수 입니다.")
        @Min(value = 1, message = "평점은 1점 이상이여아 합니다.")
        @Max(value = 5, message = "평점은 5점 이하여야 합니다.")
        Integer rating,

        String content,

        String imageUrl
){
}