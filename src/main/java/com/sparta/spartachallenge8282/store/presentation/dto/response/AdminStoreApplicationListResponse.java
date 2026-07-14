package com.sparta.spartachallenge8282.store.presentation.dto.response;

import com.sparta.spartachallenge8282.store.domain.Store;
import com.sparta.spartachallenge8282.store.domain.StoreApplication;
import com.sparta.spartachallenge8282.store.domain.StoreApplicationStatus;
import com.sparta.spartachallenge8282.user.domain.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminStoreApplicationListResponse(
        UUID applicationId,
        String storeName,
        StoreApplicationStatus status,

        Long applicantId,
        String applicantEmail,
        String applicantNickname,
        UserRole applicantRole,

        LocalDateTime createdAt,
        LocalDateTime approvedAt,
        LocalDateTime rejectedAt
) {
    public static AdminStoreApplicationListResponse from(StoreApplication application) {
        return new AdminStoreApplicationListResponse(
                application.getId(),
                application.getStoreName(),
                application.getStatus(),

                application.getApplicant().getId(),
                application.getApplicant().getEmail(),
                application.getApplicant().getNickname(),
                application.getApplicant().getRole(),

                application.getCreatedAt(),
                application.getApprovedAt(),
                application.getRejectedAt()
        );
    }
}
