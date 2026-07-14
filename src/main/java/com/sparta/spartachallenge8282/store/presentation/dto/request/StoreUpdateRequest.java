package com.sparta.spartachallenge8282.store.presentation.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;
import java.util.UUID;

public record StoreUpdateRequest(
        UUID categoryId,

        UUID regionId,

        @Size(max = 100, message = "가게 이름은 100자 이하여야 합니다.")
        String storeName,

        @Size(max = 20, message = "가게 전화번호는 20자 이하여야 합니다.")
        @Pattern(
                regexp = "^[0-9-]+$",
                message = "전화번호는 숫자와 하이픈(-)만 입력할 수 있습니다."
        )
        String storeTel,

        @Size(max = 255, message = "가게 이미지 URL은 255자 이하여야 합니다.")
        String storeImage,

        @Size(max = 255, message = "가게 주소는 255자 이하여야 합니다.")
        String address,

        @PositiveOrZero(message = "최소 주문 금액은 0원 이상이어야 합니다.")
        Integer minOrderPrice,

        @PositiveOrZero(message = "배달비는 0원 이상이어야 합니다.")
        Integer deliveryFee,

        @PositiveOrZero(message = "무료 배달 기준 금액은 0원 이상이어야 합니다.")
        Integer freeDeliveryAmount,

        LocalTime openTime,

        LocalTime closeTime
) {
}
