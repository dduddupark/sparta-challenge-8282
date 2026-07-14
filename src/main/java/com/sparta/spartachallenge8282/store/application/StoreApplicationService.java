package com.sparta.spartachallenge8282.store.application;

import com.sparta.spartachallenge8282.category.domain.Category;
import com.sparta.spartachallenge8282.category.domain.CategoryRepository;
import com.sparta.spartachallenge8282.global.common.PageResponse;
import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import com.sparta.spartachallenge8282.region.domain.Region;
import com.sparta.spartachallenge8282.region.domain.RegionRepository;
import com.sparta.spartachallenge8282.store.application.validator.StoreAuthorizationValidator;
import com.sparta.spartachallenge8282.store.domain.StoreApplication;
import com.sparta.spartachallenge8282.store.domain.StoreApplicationRepository;
import com.sparta.spartachallenge8282.store.presentation.dto.request.StoreApplicationRequest;
import com.sparta.spartachallenge8282.store.presentation.dto.response.MyStoreApplicationCreateResponse;
import com.sparta.spartachallenge8282.store.presentation.dto.response.MyStoreApplicationDetailResponse;
import com.sparta.spartachallenge8282.store.presentation.dto.response.MyStoreApplicationListResponse;
import com.sparta.spartachallenge8282.user.domain.User;
import com.sparta.spartachallenge8282.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoreApplicationService {

    private final UserRepository userRepository;
    private final StoreApplicationRepository storeApplicationRepository;
    private final CategoryRepository categoryRepository;
    private final RegionRepository regionRepository;
    private final StoreAuthorizationValidator validator;


    //가게 등록 및 조회 절차
    /**
     * 가게 등록 신청
     *
     * CUSTOMER와 OWNER 모두 신청할 수 있다.
     */
    @Transactional
    public MyStoreApplicationCreateResponse createStoreApplication(
            StoreApplicationRequest request,
            UserDetailsImpl userDetails
    ) {
        validator.validateStoreApplicationRole(userDetails);
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() ->
                        new CustomException(ErrorCode.CATEGORY_NOT_FOUND)
                );

        Region region = regionRepository.findById(request.regionId())
                .orElseThrow(() ->
                        new CustomException(ErrorCode.REGION_NOT_FOUND)
                );
        User applicantUser = userRepository.findById(userDetails.userId())
                .orElseThrow(() ->
                        new CustomException(ErrorCode.USER_NOT_FOUND)
                );


        StoreApplication application = StoreApplication.builder()
                .applicant(applicantUser)
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

        return MyStoreApplicationCreateResponse.from(storeApplicationRepository.save(application));
    }

    /**
     * 본인의 가게 등록 신청 목록 조회
     */
    @Transactional(readOnly = true)
    public PageResponse<MyStoreApplicationListResponse> getMyStoreApplications(
            UserDetailsImpl userDetails,
            Pageable pageable
    ) {
        return PageResponse.from(
                storeApplicationRepository.findAllByApplicant_Id(userDetails.userId(), pageable)
                        .map(MyStoreApplicationListResponse::from)
        );
    }

    /**
     * 본인의 가게 등록 신청 상세 조회
     */
    @Transactional(readOnly = true)
    public MyStoreApplicationDetailResponse getMyStoreApplication(
            UUID applicationId,
            UserDetailsImpl userDetails
    ) {
        return storeApplicationRepository
                .findByIdAndApplicant_Id(applicationId, userDetails.userId())
                .map(MyStoreApplicationDetailResponse::from)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.STORE_APPLICATION_NOT_FOUND)
                );
    }
}
