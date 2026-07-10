package com.sparta.spartachallenge8282.store.presentation.dto.response;

import com.sparta.spartachallenge8282.store.domain.Store;
import com.sparta.spartachallenge8282.store.domain.StoreStatus;
import com.sparta.spartachallenge8282.user.entity.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record StoreApplicationProcessResponse(
        UUID storeId,
        String storeName,
        StoreStatus storeStatus,

        Long ownerId,
        String ownerEmail,
        UserRole ownerRole,

        LocalDateTime approvedAt,
        LocalDateTime rejectedAt,
        String rejectionReason

) {
    public static StoreApplicationProcessResponse from(Store store) {
        return new StoreApplicationProcessResponse(
                store.getId(),
                store.getStoreName(),
                store.getStoreStatus(),

                store.getOwner().getId(),
                store.getOwner().getEmail(),
                store.getOwner().getRole(),

                store.getApprovedAt(),
                store.getRejectedAt(),
                store.getRejectionReason()
        );
    }
}
