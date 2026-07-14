package com.sparta.spartachallenge8282.store.presentation.dto.response;

import com.sparta.spartachallenge8282.store.domain.Store;
import com.sparta.spartachallenge8282.store.domain.StoreApplication;
import com.sparta.spartachallenge8282.store.domain.StoreApplicationStatus;
import com.sparta.spartachallenge8282.user.domain.UserRole;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record AdminStoreApplicationDetailResponse(
        UUID applicationId,

        Long applicantId,
        String applicantEmail,
        String applicantNickname,
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

        StoreApplicationStatus status,

        LocalDateTime createdAt,
        LocalDateTime approvedAt,
        LocalDateTime rejectedAt,
        String rejectionReason
) {
    public static AdminStoreApplicationDetailResponse from(StoreApplication application) {
        return new AdminStoreApplicationDetailResponse(
                application.getId(),

                application.getApplicant().getId(),
                application.getApplicant().getEmail(),
                application.getApplicant().getNickname(),
                application.getApplicant().getRole(),

                application.getCategory().getId(),
                application.getCategory().getName(),

                application.getRegion().getId(),
                application.getRegion().getName(),

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
