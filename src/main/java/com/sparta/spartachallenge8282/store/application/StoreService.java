package com.sparta.spartachallenge8282.store.application;

import com.sparta.spartachallenge8282.global.common.PageResponse;
import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.menu.domain.MenuRepository;
import com.sparta.spartachallenge8282.menu.domain.PreviewMenuProjection;
import com.sparta.spartachallenge8282.store.domain.*;
import com.sparta.spartachallenge8282.store.presentation.dto.request.StoresSearchCondition;
import com.sparta.spartachallenge8282.store.presentation.dto.response.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {
    private final StoreRepository storeRepository;
    private final MenuRepository menuRepository;

    //일반 사용자 및 비회원 사용자의 가게 조회 ==========================

    /**
     * 가게 목록 조회
     * 활성화된 가게만 노출시킨다.
     */
    public PageResponse<UserStoreListResponse> getStores(StoresSearchCondition condition, Pageable pageable) {
        Page<Store> stores = storeRepository.searchStores(condition, pageable);

        List<UUID> storeIds =
                stores.getContent()
                        .stream()
                        .map(Store::getId)
                        .toList();

        if(storeIds.isEmpty()) {
            Page<UserStoreListResponse> emptyResponse =
                    stores.map(store ->
                            UserStoreListResponse.from(store, List.of()));
            return PageResponse.from(emptyResponse);
        }

        List<PreviewMenuProjection> menus =
                menuRepository.findTop3MenusByStoreIds(storeIds);

        Map<UUID, List<PreviewMenuProjection>> menuMap =
                menus.stream().collect(Collectors.groupingBy(PreviewMenuProjection::getStoreId));

        Page<UserStoreListResponse> response =
                stores.map(store ->
                            UserStoreListResponse.from(store, menuMap.getOrDefault(store.getId(), List.of()))
                        );

        return PageResponse.from(response);
    }

    /**
     * 가게 상세 조회
     * 활성화된 가게만 노출시킨다.
     */
    public UserStoreDetailResponse getStore(UUID storeId) {
        Store store =
                storeRepository
                        .findByIdAndOperationStatusAndDeletedAtIsNull(
                                storeId,
                                StoreOperationStatus.ACTIVE)
                        .orElseThrow(()->
                                new CustomException(ErrorCode.STORE_NOT_FOUND)
                        );
        return  UserStoreDetailResponse.from(store);
    }


}
