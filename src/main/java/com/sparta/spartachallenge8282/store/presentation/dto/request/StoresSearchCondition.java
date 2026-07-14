package com.sparta.spartachallenge8282.store.presentation.dto.request;

import com.sparta.spartachallenge8282.store.domain.StoreSortType;

import java.util.UUID;

public record StoresSearchCondition(
        String keyword,
        UUID categoryId,
        UUID regionId,
        Boolean isOpen,
        StoreSortType sortType
) {


}
