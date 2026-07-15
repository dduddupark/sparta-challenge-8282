package com.sparta.spartachallenge8282.store.domain;

import com.sparta.spartachallenge8282.store.presentation.dto.request.StoresSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StoreRepositoryCustom {

    Page<Store> searchStores(
            StoresSearchCondition condition,
            Pageable pageable
    );
}
