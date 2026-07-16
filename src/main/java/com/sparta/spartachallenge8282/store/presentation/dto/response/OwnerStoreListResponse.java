package com.sparta.spartachallenge8282.store.presentation.dto.response;

import com.sparta.spartachallenge8282.store.domain.Store;
import com.sparta.spartachallenge8282.store.domain.StoreOperationStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record OwnerStoreListResponse(
        UUID storeId,
        String storeName,
        String storeImage,

        UUID categoryId,
        String categoryName,

        UUID regionId,
        String regionName,

        String address,

        BigDecimal storeRating,
        Integer reviewCount,

        StoreOperationStatus operationStatus,
        boolean isOpen
) {
    public static OwnerStoreListResponse from(Store store) {
        return new OwnerStoreListResponse(
                store.getId(),
                store.getStoreName(),
                store.getStoreImage(),

                store.getCategory().getId(),
                store.getCategory().getName(),

                store.getRegion().getId(),
                store.getRegion().getName(),

                store.getAddress(),


                store.getStoreRating(),
                store.getReviewCount(),

                store.getOperationStatus(),
                store.isOpen()
        );
    }
}
