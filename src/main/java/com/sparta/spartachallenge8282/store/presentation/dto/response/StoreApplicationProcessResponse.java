package com.sparta.spartachallenge8282.store.presentation.dto.response;

import com.sparta.spartachallenge8282.store.domain.Store;
import com.sparta.spartachallenge8282.store.domain.StoreApplication;
import com.sparta.spartachallenge8282.store.domain.StoreApplicationStatus;
import com.sparta.spartachallenge8282.user.domain.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record StoreApplicationProcessResponse(
        UUID applicationId,
        UUID storeId,
        String storeName,
        StoreApplicationStatus status,

        Long ownerId,
        String ownerEmail,
        UserRole ownerRole,

        LocalDateTime approvedAt,
        LocalDateTime rejectedAt,
        String rejectionReason

) {
    public static StoreApplicationProcessResponse from(StoreApplication application, Store store) {
        return new StoreApplicationProcessResponse(
                application.getId(),
                store.getId(),
                application.getStoreName(),
                application.getStatus(),

                application.getApplicant().getId(),
                application.getApplicant().getEmail(),
                application.getApplicant().getRole(),

                application.getApprovedAt(),
                application.getRejectedAt(),
                application.getRejectionReason()
        );
    }

    //거절 시 store는 생성되지 않음
    public static StoreApplicationProcessResponse from(StoreApplication application) {
        return new StoreApplicationProcessResponse(
                application.getId(),
                null,
                application.getStoreName(),
                application.getStatus(),


                application.getApplicant().getId(),
                application.getApplicant().getEmail(),
                application.getApplicant().getRole(),

                application.getApprovedAt(),
                application.getRejectedAt(),
                application.getRejectionReason()
        );
    }


}
