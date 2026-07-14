package com.sparta.spartachallenge8282.store.presentation.dto.response;

import com.sparta.spartachallenge8282.store.domain.Store;
import com.sparta.spartachallenge8282.store.domain.StoreApplication;
import com.sparta.spartachallenge8282.store.domain.StoreApplicationStatus;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record MyStoreApplicationDetailResponse(
        UUID applicationId,
        UUID categoryId,
        UUID regionId,
        String storeName,
        String storeTel,
        String storeImage,
        String address,
        Integer minOrderPrice,
        Integer deliveryFee,
        Integer freeDeliveryAmount,
        LocalTime openTime,
        LocalTime closeTime,
        StoreApplicationStatus storeStatus,
        LocalDateTime createdAt,
        LocalDateTime approvedAt,
        LocalDateTime rejectedAt,
        String rejectionReason

) {
    public static MyStoreApplicationDetailResponse from(StoreApplication application) {
        return new MyStoreApplicationDetailResponse(
                application.getId(),

                application.getCategory().getId(),
                application.getRegion().getId(),

                application.getStoreName(),
                application.getStoreTel(),
                application.getStoreImage(),
                application.getAddress(),

                application.getMinOrderPrice(),
                application.getDeliveryFee(),
                application.getFreeDeliveryAmount(),

                application.getOpenTime(),
                application.getCloseTime(),

                application.getStatus(),

                application.getCreatedAt(),
                application.getApprovedAt(),
                application.getRejectedAt(),
                application.getRejectionReason()
        );
    }
}
