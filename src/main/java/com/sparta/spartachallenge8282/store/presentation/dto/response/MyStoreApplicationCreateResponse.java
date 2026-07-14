package com.sparta.spartachallenge8282.store.presentation.dto.response;

import com.sparta.spartachallenge8282.store.domain.Store;
import com.sparta.spartachallenge8282.store.domain.StoreApplication;
import com.sparta.spartachallenge8282.store.domain.StoreApplicationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record MyStoreApplicationCreateResponse(
        UUID storeId,
        String storeName,
        StoreApplicationStatus storeStatus,
        LocalDateTime appliedAt
) {
    public static MyStoreApplicationCreateResponse from(StoreApplication application) {
        return new MyStoreApplicationCreateResponse(
                application.getId(),
                application.getStoreName(),
                application.getStatus(),
                application.getCreatedAt()
        );
    }
}
