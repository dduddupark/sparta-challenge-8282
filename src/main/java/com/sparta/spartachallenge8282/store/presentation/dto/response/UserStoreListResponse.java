package com.sparta.spartachallenge8282.store.presentation.dto.response;

import com.sparta.spartachallenge8282.store.domain.Store;

import java.math.BigDecimal;
import java.util.UUID;

public record UserStoreListResponse(
        UUID storeId,

        String storeName,
        String storeImage,

        String categoryName,

        BigDecimal storeRating,
        Integer reviewCount,

        Integer deliveryFee,
        Integer minOrderPrice,

        boolean isOpen
) {
    public static UserStoreListResponse from(Store store) {
        return new UserStoreListResponse(
                store.getId(),

                store.getStoreName(),
                store.getStoreImage(),

                store.getCategory().getName(),

                store.getStoreRating(),
                store.getReviewCount(),

                store.getDeliveryFee(),
                store.getMinOrderPrice(),

                store.isOpen()
        );
    }
}
