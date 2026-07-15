package com.sparta.spartachallenge8282.store.application;

import com.sparta.spartachallenge8282.category.domain.Category;
import com.sparta.spartachallenge8282.category.domain.CategoryRepository;
import com.sparta.spartachallenge8282.global.common.PageResponse;
import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import com.sparta.spartachallenge8282.menu.domain.MenuRepository;
import com.sparta.spartachallenge8282.region.domain.Region;
import com.sparta.spartachallenge8282.region.domain.RegionRepository;
import com.sparta.spartachallenge8282.store.domain.Store;
import com.sparta.spartachallenge8282.store.domain.StoreOperationStatus;
import com.sparta.spartachallenge8282.store.domain.StoreRepository;
import com.sparta.spartachallenge8282.store.presentation.dto.request.StoreOpenStatusRequest;
import com.sparta.spartachallenge8282.store.presentation.dto.request.StoreUpdateRequest;
import com.sparta.spartachallenge8282.store.presentation.dto.response.OwnerStoreDetailResponse;
import com.sparta.spartachallenge8282.store.presentation.dto.response.OwnerStoreListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OwnerStoreService {

    private final StoreRepository storeRepository;
    private final MenuRepository menuRepository;
    private final CategoryRepository categoryRepository;
    private final RegionRepository regionRepository;

    /**
     * 본인 가게 목록 조회
     */
    @Transactional(readOnly = true)
    public PageResponse<OwnerStoreListResponse> getMyStores(UserDetailsImpl userDetails, Pageable pageable) {
        Page<Store> stores = storeRepository.findAllByOwnerIdAndDeletedAtIsNull(userDetails.userId(), pageable);
        return PageResponse.from(stores.map(OwnerStoreListResponse::from));
    }

    /**
     * 본인 가게 상세 조회
     */
    @Transactional(readOnly = true)
    public OwnerStoreDetailResponse getMyStore(UUID storeId, UserDetailsImpl userDetails) {
        Store store = storeRepository
                .findByIdAndOwner_IdAndDeletedAtIsNull(storeId, userDetails.userId())
                .orElseThrow(() ->
                        new CustomException(ErrorCode.STORE_NOT_FOUND)
                );
        return OwnerStoreDetailResponse.from(store);
    }


    // 가게 활성화
    /**
     * 메뉴가 한 개이상 있을 때 가게 활성화 가능
     */
    @Transactional
    public void activateStore(UUID storeId, UserDetailsImpl userDetails) {
        Store store = storeRepository
                .findByIdAndOwner_IdAndDeletedAtIsNull(
                        storeId,
                        userDetails.userId()
                )
                .orElseThrow(() ->
                        new CustomException(ErrorCode.STORE_NOT_APPROVED)
                );
        if (store.getOperationStatus() != StoreOperationStatus.PREPARING) {
            throw new CustomException(ErrorCode.STORE_ACTIVATION_NOT_ALLOWED);
        }

        boolean hasMenu = menuRepository.existsByStoreIdAndDeletedAtIsNullAndIsHiddenFalse(storeId);
        if(!hasMenu) {
            throw new CustomException(ErrorCode.STORE_MENU_REQUIRED);
        }
        store.activate();
    }

    /**
     * ACTIVE 상테일 때 가게를 오픈하여 주문을 받을 수 있다.
     * 가게 영업을 종료할 수 있다..
     */
    @Transactional
    public void changeOpenStatus(UUID storeId, StoreOpenStatusRequest request, UserDetailsImpl userDetails) {
        Store store = storeRepository
                .findByIdAndOwner_IdAndDeletedAtIsNull(
                        storeId,
                        userDetails.userId()
                )
                .orElseThrow(()->
                        new CustomException(ErrorCode.STORE_NOT_FOUND)
                );
        if (store.getOperationStatus() != StoreOperationStatus.ACTIVE) {
            throw new CustomException(ErrorCode.STORE_NOT_ACTIVE);
        }
        store.changeOpenStatus(request.isOpen());
    }

    /**
     * 가게 정보 수정하기
     */
    @Transactional
    public OwnerStoreDetailResponse updateStore(UUID storeId, StoreUpdateRequest request, UserDetailsImpl userDetails) {
        Store store = storeRepository
                .findByIdAndOwner_IdAndDeletedAtIsNull(storeId, userDetails.userId())
                .orElseThrow(() ->
                        new CustomException(ErrorCode.STORE_NOT_FOUND));

        //삭제 요청 상태이거나 삭제 상태인 경우 수정 불가
        if (store.getOperationStatus() == StoreOperationStatus.CLOSE_REQUESTED
                || store.getOperationStatus() == StoreOperationStatus.CLOSED) {
            throw new CustomException(
                    ErrorCode.STORE_UPDATE_NOT_ALLOWED
            );
        }

        Category category = null;
        if(request.categoryId() != null){
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() ->
                            new CustomException(ErrorCode.CATEGORY_NOT_FOUND));
        }

        Region region = null;
        if(request.regionId() != null){
            region = regionRepository.findById(request.regionId())
                    .orElseThrow(() ->
                            new CustomException(ErrorCode.REGION_NOT_FOUND));
        }


        store.update(
                category,
                region,
                request.storeName(),
                request.storeTel(),
                request.storeImage(),
                request.address(),
                request.minOrderPrice(),
                request.deliveryFee(),
                request.freeDeliveryAmount(),
                request.openTime(),
                request.closeTime()
        );

        return OwnerStoreDetailResponse.from(store);
    }


    /**
     * 가게 삭제 요청
     */
    @Transactional
    public void requestDeleteStore(UUID storeId, UserDetailsImpl userDetails) {
        Store store = storeRepository
                .findByIdAndOwner_IdAndDeletedAtIsNull(storeId, userDetails.userId())
                .orElseThrow(() ->
                        new CustomException(ErrorCode.STORE_NOT_FOUND));

        if(store.getOperationStatus() == StoreOperationStatus.CLOSE_REQUESTED){
            throw new CustomException(ErrorCode.STORE_CLOSE_ALREADY_REQUESTED);
        }
        store.requestDelete();
    }



}
