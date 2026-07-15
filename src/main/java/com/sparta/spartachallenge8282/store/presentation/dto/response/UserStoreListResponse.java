package com.sparta.spartachallenge8282.store.presentation.dto.response;

import com.sparta.spartachallenge8282.menu.domain.PreviewMenuProjection;
import com.sparta.spartachallenge8282.menu.presentation.dto.response.StorePreviewMenuResponse;
import com.sparta.spartachallenge8282.store.domain.Store;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record UserStoreListResponse(
        UUID storeId,

        String storeName,
        String storeImage,

        String categoryName,
        String regionName,

        BigDecimal storeRating,
        Integer reviewCount,

        Integer deliveryFee,
        Integer minOrderPrice,

        boolean isOpen,

        List<StorePreviewMenuResponse> menus
) {
    public static UserStoreListResponse from(Store store, List<PreviewMenuProjection> menus) {

        List<StorePreviewMenuResponse> previewMenus =
                menus.stream()
                        .map(StorePreviewMenuResponse::from)
                        .toList();

        return new UserStoreListResponse(
                store.getId(),

                store.getStoreName(),
                store.getStoreImage(),

                store.getCategory().getName(),
                store.getRegion().getName(),

                store.getStoreRating(),
                store.getReviewCount(),

                store.getDeliveryFee(),
                store.getMinOrderPrice(),

                store.isOpen(),

                previewMenus


        );
    }
}
