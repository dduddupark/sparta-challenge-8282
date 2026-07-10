package com.sparta.spartachallenge8282.store.application;

import com.sparta.spartachallenge8282.category.domain.Category;
import com.sparta.spartachallenge8282.category.domain.CategoryRepository;
import com.sparta.spartachallenge8282.global.common.PageResponse;
import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import com.sparta.spartachallenge8282.region.domain.Region;
import com.sparta.spartachallenge8282.region.domain.RegionRepository;
import com.sparta.spartachallenge8282.store.domain.Store;
import com.sparta.spartachallenge8282.store.domain.StoreRepository;
import com.sparta.spartachallenge8282.store.domain.StoreStatus;
import com.sparta.spartachallenge8282.store.presentation.dto.request.StoreApplicationRequest;
import com.sparta.spartachallenge8282.store.presentation.dto.request.StoreRejectRequest;
import com.sparta.spartachallenge8282.store.presentation.dto.response.*;
import com.sparta.spartachallenge8282.user.entity.User;
import com.sparta.spartachallenge8282.user.entity.UserRole;
import com.sparta.spartachallenge8282.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoreService {
    private final StoreRepository storeRepository;
    private final RegionRepository regionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    //가게 등록 및 조회 절차
    /**
     * 가게 등록 신청
     *
     * CUSTOMER와 OWNER 모두 신청할 수 있다.
     */
    @Transactional
    public MyStoreApplicationCreateResponse createStore(
            StoreApplicationRequest request,
            UserDetailsImpl userDetails
    ) {
       validateStoreApplicationRole(userDetails);
       Category category = categoryRepository.findById(request.categoryId())
               .orElseThrow(() ->
                       new CustomException(ErrorCode.CATEGORY_NOT_FOUND)
               );

       Region region = regionRepository.findById(request.regionId())
               .orElseThrow(() ->
                       new CustomException(ErrorCode.REGION_NOT_FOUND)
               );
       User user = userRepository.findById(userDetails.userId())
                .orElseThrow(() ->
                        new CustomException(ErrorCode.USER_NOT_FOUND)
                );

        Store store = Store.builder()
                .owner(user)
                .category(category)
                .region(region)
                .storeName(request.storeName())
                .storeTel(request.storeTel())
                .storeImage(request.storeImage())
                .address(request.address())
                .minOrderPrice(request.minOrderPrice())
                .deliveryFee(request.deliveryFee())
                .freeDeliveryAmount(request.freeDeliveryAmount())
                .openTime(request.openTime())
                .closeTime(request.closeTime())
                .build();

        return MyStoreApplicationCreateResponse.from(storeRepository.save(store));
    }

    /**
     * 본인의 가게 등록 신청 목록 조회
     */
    @Transactional(readOnly = true)
    public PageResponse<StoreApplicationListResponse> getMyStoreApplications(
            UserDetailsImpl userDetails,
            Pageable pageable
    ) {
        return PageResponse.from(
                storeRepository
                        .findAllByOwner_Id(userDetails.userId(), pageable)
                        .map(StoreApplicationListResponse::from)
        );
    }

    /**
     * 본인의 가게 등록 신청 상세 조회
     */
    @Transactional(readOnly = true)
    public MyStoreApplicationDetailResponse getMyStoreApplication(
            UUID storeId,
            UserDetailsImpl userDetails
    ) {
        return storeRepository
                .findByIdAndOwner_Id(storeId, userDetails.userId())
                .map(MyStoreApplicationDetailResponse::from)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.STORE_NOT_FOUND)
                );
    }


    //관리자의 가게 조회 및 등록 승인/거절 절차 ===================

    /**
     * 관리자 가게 등록 신청 목록 조회
     */
    public PageResponse<AdminStoreApplicationListResponse> getAdminStoreApplications(StoreStatus status, Pageable pageable, UserDetailsImpl userDetails) {
        validateManagerRole(userDetails);
        Page<Store> stores;

        if(status == null){
            stores = storeRepository.findAll(pageable);
        }else{
            stores = storeRepository.findAllByStoreStatus(status, pageable);
        }
        return PageResponse.from(stores.map(AdminStoreApplicationListResponse::from));
    }

    /**
     * 관리자 가게 등록 신청 상세 조회
     */
    public AdminStoreApplicationDetailResponse getAdminStoreApplication(UUID storeId, UserDetailsImpl userDetails) {
        validateManagerRole(userDetails);

        Store store = storeRepository.findById(storeId)
                .orElseThrow(()->
                        new CustomException(ErrorCode.STORE_NOT_FOUND));
        return AdminStoreApplicationDetailResponse.from(store);
    }


    /**
     * 가게 등록 신청 승인
     *
     * MANAGER 또는 MASTER만 가능하다.
     */
    @Transactional
    public StoreApplicationProcessResponse approveStore(UUID storeId, UserDetailsImpl userDetails) {
        validateManagerRole(userDetails);

        Store store = storeRepository.findById(storeId)
                .orElseThrow(()->
                        new CustomException(ErrorCode.STORE_NOT_FOUND)
                );

        //승인 또는 거절은 PENDING 상태에서만 가능하게 한다.
        if (store.getStoreStatus() != StoreStatus.PENDING) {
            throw new CustomException(ErrorCode.INVALID_STORE_STATUS);
        }
        store.approve();
        store.getOwner().promoteToOwner();
        return StoreApplicationProcessResponse.from(store);
    }

    /**
     * 가게 등록 신청 거절
     *
     * MANAGER 또는 MASTER만 가능하다.
     * 거절 시 사용자 권한은 변경하지 않는다.
     */
    @Transactional
    public StoreApplicationProcessResponse rejectStore(UUID storeId, StoreRejectRequest request, UserDetailsImpl userDetails) {
        validateManagerRole(userDetails);

        Store store = storeRepository.findById(storeId)
                .orElseThrow(()->
                        new CustomException(ErrorCode.STORE_NOT_FOUND)
                );
        //승인 또는 거절은 PENDING 상태에서만 가능하게 한다.
        if (store.getStoreStatus() != StoreStatus.PENDING) {
            throw new CustomException(ErrorCode.INVALID_STORE_STATUS);
        } else if (request.rejectionReason() == null || request.rejectionReason().isBlank()) {
            throw new CustomException(ErrorCode.REJECTION_REASON_REQUIRED);
        }

        store.reject(request.rejectionReason());
        return StoreApplicationProcessResponse.from(store);



    }

    //권한 검증 ==============
    /**
     * 가게 등록 신청 권한 검사
     *
     * CUSTOMER와 OWNER 모두 신청 가능하다.
     */
    private void validateStoreApplicationRole(UserDetailsImpl userDetails) {
        boolean isCustomer = UserRole.CUSTOMER.getAuthority().equals(userDetails.role());
        boolean isOwner = UserRole.OWNER.getAuthority().equals(userDetails.role());
        if(!isCustomer && !isOwner){
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
    }



    /**
     * 관리자 승인·거절 권한 검사
     */
    private void validateManagerRole(UserDetailsImpl userDetails) {
        boolean isManager = UserRole.MANAGER.getAuthority().equals(userDetails.role());
        boolean isMaster = UserRole.MASTER.getAuthority().equals(userDetails.role());

        if(!isManager && !isMaster){
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
    }



}
