package com.sparta.spartachallenge8282.store.presentation.dto.response;

import com.sparta.spartachallenge8282.store.domain.Store;
import com.sparta.spartachallenge8282.store.domain.StoreStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record MyStoreApplicationCreateResponse(
        UUID storeId,
        String storeName,
        StoreStatus storeStatus,
        LocalDateTime appliedAt
) {
    public static MyStoreApplicationCreateResponse from(Store store) {
        return new MyStoreApplicationCreateResponse(
                store.getId(),
                store.getStoreName(),
                store.getStoreStatus(),
                store.getCreatedAt()
        );
    }
}
