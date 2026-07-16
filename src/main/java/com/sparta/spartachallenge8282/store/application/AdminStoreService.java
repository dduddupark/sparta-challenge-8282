package com.sparta.spartachallenge8282.store.application;

import com.sparta.spartachallenge8282.global.common.PageResponse;
import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import com.sparta.spartachallenge8282.store.application.validator.StoreAuthorizationValidator;
import com.sparta.spartachallenge8282.store.domain.*;
import com.sparta.spartachallenge8282.store.presentation.dto.request.StoreRejectRequest;
import com.sparta.spartachallenge8282.store.presentation.dto.response.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminStoreService {
    private final StoreApplicationRepository storeApplicationRepository;
    private final StoreRepository storeRepository;
    private final StoreAuthorizationValidator validator;


    //관리자의 가게 조회 및 등록 승인/거절 절차 ===================

    /**
     * 관리자 가게 등록 신청 목록 조회
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminStoreApplicationListResponse> getAdminStoreApplications(StoreApplicationStatus status, Pageable pageable) {

        Page<StoreApplication> applications;

        if(status == null){
            applications = storeApplicationRepository.findAll(pageable);
        }else{
            applications = storeApplicationRepository.findAllByStatus(status, pageable);
        }
        return PageResponse.from(applications.map(AdminStoreApplicationListResponse::from));
    }

    /**
     * 관리자 가게 등록 신청 상세 조회
     */
    @Transactional(readOnly = true)
    public AdminStoreApplicationDetailResponse getAdminStoreApplication(UUID applicationId) {

        StoreApplication application = storeApplicationRepository.findById(applicationId)
                .orElseThrow(()->
                        new CustomException(ErrorCode.STORE_APPLICATION_NOT_FOUND));
        return AdminStoreApplicationDetailResponse.from(application);
    }

    /**
     * 등록 승인되어 관리되는 가게 목록 조회
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminStoreListResponse> getStores(StoreOperationStatus status, Pageable pageable) {
          Page<Store> stores;


          if(status == null){
              stores = storeRepository.findAll(pageable); //관리자는 삭제된 가게도 조회
          }else{
              stores = storeRepository.findAllByOperationStatus(status, pageable);

          }
        return PageResponse.from(stores.map(AdminStoreListResponse::from));


    }

    /**
     * 등록 승인되어 관리되는 가게 상세 조회
     */
    @Transactional(readOnly = true)
    public AdminStoreDetailResponse getStore(UUID storeId) {
        Store store = storeRepository
                .findById(storeId)
                .orElseThrow(()->
                        new CustomException(ErrorCode.STORE_NOT_FOUND)
                );
        return AdminStoreDetailResponse.from(store);

    }


    /**
     * 가게 등록 신청 승인
     *
     * MANAGER 또는 MASTER만 가능하다.
     */
    @Transactional
    public StoreApplicationProcessResponse approveStore(UUID applicationId, UserDetailsImpl userDetails) {
        validator.validateManagerRole(userDetails);

        StoreApplication application = storeApplicationRepository.findById(applicationId)
                .orElseThrow(()->
                        new CustomException(ErrorCode.STORE_APPLICATION_NOT_FOUND)
                );

        //승인 또는 거절은 PENDING 상태에서만 가능하게 한다.
        if (application.getStatus() != StoreApplicationStatus.PENDING) {
            throw new CustomException(ErrorCode.INVALID_STORE_APPLICATION_STATUS);
        }
        application.approve();

        Store savedStore = storeRepository.save(Store.from(application));

        application.getApplicant().promoteToOwner();
        return StoreApplicationProcessResponse.from(application, savedStore);
    }

    /**
     * 가게 등록 신청 거절
     *
     * MANAGER 또는 MASTER만 가능하다.
     * 거절 시 사용자 권한은 변경하지 않는다.
     */
    @Transactional
    public StoreApplicationProcessResponse rejectStore(UUID applicationId, StoreRejectRequest request, UserDetailsImpl userDetails) {
        validator.validateManagerRole(userDetails);

        StoreApplication application = storeApplicationRepository.findById(applicationId)
                .orElseThrow(()->
                        new CustomException(ErrorCode.STORE_APPLICATION_NOT_FOUND)
                );
        //승인 또는 거절은 PENDING 상태에서만 가능하게 한다.
        if (application.getStatus() != StoreApplicationStatus.PENDING) {
            throw new CustomException(ErrorCode.INVALID_STORE_APPLICATION_STATUS);
        } else if (request.rejectionReason() == null || request.rejectionReason().isBlank()) {
            throw new CustomException(ErrorCode.REJECTION_REASON_REQUIRED);
        }

        application.reject(request.rejectionReason());
        return StoreApplicationProcessResponse.from(application);
    }

    /**
     * 가게 삭제 승인
     */
    @Transactional
    public void approveDeleteStore(UUID storeId, UserDetailsImpl userDetails) {
        validator.validateManagerRole(userDetails);
        Store store = storeRepository.findByIdAndDeletedAtIsNull(storeId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.STORE_NOT_FOUND)
                );
        //삭제 요청 상태일 때 삭제 승인이 가능
        if(store.getOperationStatus() != StoreOperationStatus.CLOSE_REQUESTED){
            throw new CustomException(
                    ErrorCode.STORE_CLOSE_NOT_REQUESTED
            );
        }
        store.approveDelete(userDetails.userId());
    }


}
