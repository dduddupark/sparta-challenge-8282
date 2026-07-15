package com.sparta.spartachallenge8282.store.presentation.dto.response;


import com.sparta.spartachallenge8282.store.domain.StoreApplication;
import com.sparta.spartachallenge8282.store.domain.StoreApplicationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record MyStoreApplicationListResponse(
        Long applicantId,
        UUID applicationId,
        String storeName,
        String address,
        StoreApplicationStatus status,
        LocalDateTime createdAt,
        LocalDateTime approvedAt,
        LocalDateTime rejectedAt

) {
    public static MyStoreApplicationListResponse from(StoreApplication application) {
        return new MyStoreApplicationListResponse(
                application.getApplicant().getId(),
                application.getId(),
                application.getStoreName(),
                application.getAddress(),
                application.getStatus(),
                application.getCreatedAt(),
                application.getApprovedAt(),
                application.getRejectedAt()
        );
    }

}
