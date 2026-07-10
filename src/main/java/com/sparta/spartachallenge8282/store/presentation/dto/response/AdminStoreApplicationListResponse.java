package com.sparta.spartachallenge8282.store.presentation.dto.response;

import com.sparta.spartachallenge8282.store.domain.Store;
import com.sparta.spartachallenge8282.store.domain.StoreStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminStoreApplicationListResponse(
        UUID storeId,
        String storeName,
        String storeImage,
        String categoryName,
        String regionName,

        Long applicantId,
        String applicantNickname,
        String applicantEmail,

        StoreStatus storeStatus,
        LocalDateTime appliedAt
) {
    public static AdminStoreApplicationListResponse from(Store store) {
        return new AdminStoreApplicationListResponse(
                store.getId(),
                store.getStoreName(),
                store.getStoreImage(),
                store.getCategory().getName(),
                store.getRegion().getName(),

                store.getOwner().getId(),
                store.getOwner().getNickname(),
                store.getOwner().getEmail(),

                store.getStoreStatus(),
                store.getCreatedAt()
        );
    }
}
