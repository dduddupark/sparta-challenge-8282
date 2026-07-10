package com.sparta.spartachallenge8282.store.presentation.dto.response;

import com.sparta.spartachallenge8282.store.domain.Store;
import com.sparta.spartachallenge8282.store.domain.StoreStatus;
import com.sparta.spartachallenge8282.user.entity.UserRole;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record AdminStoreApplicationDetailResponse(
        UUID storeId,

        Long applicantId,
        String applicantNickname,
        String applicantEmail,
        UserRole applicantRole,

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

        LocalTime openTime,
        LocalTime closeTime,

        StoreStatus storeStatus,

        LocalDateTime appliedAt,
        LocalDateTime approvedAt,
        LocalDateTime rejectedAt,
        String rejectionReason
) {
    public static AdminStoreApplicationDetailResponse from(Store store) {
        return new AdminStoreApplicationDetailResponse(
                store.getId(),

                store.getOwner().getId(),
                store.getOwner().getNickname(),
                store.getOwner().getEmail(),
                store.getOwner().getRole(),

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

                store.getOpenTime(),
                store.getCloseTime(),

                store.getStoreStatus(),

                store.getCreatedAt(),
                store.getApprovedAt(),
                store.getRejectedAt(),
                store.getRejectionReason()
        );
    }
}
