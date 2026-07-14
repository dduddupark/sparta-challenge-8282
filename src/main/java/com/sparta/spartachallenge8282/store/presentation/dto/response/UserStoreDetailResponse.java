package com.sparta.spartachallenge8282.store.presentation.dto.response;

import com.sparta.spartachallenge8282.store.domain.Store;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

public record UserStoreDetailResponse(
        UUID storeId,

        String storeName,
        String storeImage,

        String categoryName,
        String regionName,

        String address,
        String storeTel,

        BigDecimal storeRating,
        Integer reviewCount,

        Integer minOrderPrice,
        Integer deliveryFee,
        Integer freeDeliveryAmount,

        LocalTime openTime,
        LocalTime closeTime,

        boolean isOpen

) {
    public static UserStoreDetailResponse from(Store store) {
        return new UserStoreDetailResponse(
                store.getId(),

                store.getStoreName(),
                store.getStoreImage(),

                store.getCategory().getName(),
                store.getRegion().getName(),

                store.getAddress(),
                store.getStoreTel(),

                store.getStoreRating(),
                store.getReviewCount(),

                store.getMinOrderPrice(),
                store.getDeliveryFee(),
                store.getFreeDeliveryAmount(),

                store.getOpenTime(),
                store.getCloseTime(),

                store.isOpen()
        );
    }
}
