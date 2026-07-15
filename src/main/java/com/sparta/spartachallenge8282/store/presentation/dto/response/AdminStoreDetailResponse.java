package com.sparta.spartachallenge8282.store.presentation.dto.response;

import com.sparta.spartachallenge8282.store.domain.Store;
import com.sparta.spartachallenge8282.store.domain.StoreOperationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record AdminStoreDetailResponse(
        UUID storeId,

        Long ownerId,
        String ownerEmail,
        String ownerNickname,

        UUID categoryId,
        String categoryName,

        UUID regionId,
        String regionName,

        String storeName,
        String storeTel,
        String storeImage,
        String address,

        Integer minOrderPrice,
        Integer deliveryFee,
        Integer freeDeliveryAmount,

        BigDecimal storeRating,
        Integer reviewCount,

        LocalTime openTime,
        LocalTime closeTime,

        StoreOperationStatus operationStatus,
        boolean isOpen,

        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminStoreDetailResponse from(Store store) {
        return new AdminStoreDetailResponse(
                store.getId(),

                store.getOwner().getId(),
                store.getOwner().getEmail(),
                store.getOwner().getNickname(),

                store.getCategory().getId(),
                store.getCategory().getName(),

                store.getRegion().getId(),
                store.getRegion().getName(),

                store.getStoreName(),
                store.getStoreTel(),
                store.getStoreImage(),
                store.getAddress(),

                store.getMinOrderPrice(),
                store.getDeliveryFee(),
                store.getFreeDeliveryAmount(),

                store.getStoreRating(),
                store.getReviewCount(),

                store.getOpenTime(),
                store.getCloseTime(),

                store.getOperationStatus(),
                store.isOpen(),

                store.getCreatedAt(),
                store.getUpdatedAt()
        );
    }
}
