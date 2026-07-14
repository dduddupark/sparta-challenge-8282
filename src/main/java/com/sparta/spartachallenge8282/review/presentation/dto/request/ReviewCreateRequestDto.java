package com.sparta.spartachallenge8282.review.presentation.dto.request;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * 리뷰 작성 요청 DTO.
 * storeId 는 포함하지 않는다 — 클라이언트가 임의로 조작하지 못하도록,
 * Service 가 orderId 로 Order 를 조회해 storeId 를 직접 얻는다.
 * orderId·rating 은 필수, content·imageUrl 은 선택이다
 * (image를 여러 장 올리려면 수정이 필요함 - 현재는 사진 한 장만 올릴 수 있도록 구현)
 * (평점만 남기고 텍스트 리뷰는 생략할 수 있다).
 */

public record ReviewCreateRequestDto(
        @NotNull(message = "주문 ID는 필수입니다.")
        UUID orderId,

        @NotNull(message = "평점은 필수 입니다.")
        @Min(value = 1, message = "평점은 1점 이상이어야 합니다.")
        @Max(value = 5, message = "평점은 5점 이하여야 합니다.")
        Integer rating,

        String content,

        String imageUrl
){
}