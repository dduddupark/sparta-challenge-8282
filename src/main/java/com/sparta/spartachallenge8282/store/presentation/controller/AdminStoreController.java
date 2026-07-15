package com.sparta.spartachallenge8282.store.presentation.controller;

import com.sparta.spartachallenge8282.global.common.ApiResponse;
import com.sparta.spartachallenge8282.global.common.PageResponse;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import com.sparta.spartachallenge8282.store.application.AdminStoreService;
import com.sparta.spartachallenge8282.store.application.StoreApplicationService;
import com.sparta.spartachallenge8282.store.application.StoreService;
import com.sparta.spartachallenge8282.store.domain.StoreApplicationStatus;
import com.sparta.spartachallenge8282.store.domain.StoreOperationStatus;
import com.sparta.spartachallenge8282.store.presentation.dto.request.StoreRejectRequest;
import com.sparta.spartachallenge8282.store.presentation.dto.response.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


/**
 * 관리자의 가게 등록 관리
 */
@RestController
@RequestMapping("/api/v1/admin/stores")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_MASTER')")
public class AdminStoreController {
    private final AdminStoreService adminStoreService;


    /**
     * 등록 신청된 가게 목록 조회
     *
     * status를 전달하지 않으면 전체 조회
     * status를 전달하면 해당 상태만 조회
     */
    @GetMapping("/applications")
    public ResponseEntity<ApiResponse<PageResponse<AdminStoreApplicationListResponse>>> getStoreApplications(
            @RequestParam(required = false) StoreApplicationStatus status,
            @PageableDefault(size = 20) Pageable pageable

    ) {
        PageResponse<AdminStoreApplicationListResponse> responses =
                adminStoreService.getAdminStoreApplications(status, pageable);

        return ResponseEntity.ok(ApiResponse.success("등록 신청된 가게 목록 조회 성공", responses));
    }


    /**
     * 등록 신청된 가게 상세 조회
     */
    @GetMapping("/applications/{applicationId}")
    public ResponseEntity<ApiResponse<AdminStoreApplicationDetailResponse>> getStoreApplication(
            @PathVariable UUID applicationId
    ){
        AdminStoreApplicationDetailResponse response =
                adminStoreService.getAdminStoreApplication(applicationId);
        return ResponseEntity.ok(ApiResponse.success("등록 신청된 가게 상세 조회 성공", response));
    }

    /**
     * 가게 등록 신청 승인
     */
    @PatchMapping("/applications/{applicationId}/approve")
    public ResponseEntity<ApiResponse<StoreApplicationProcessResponse>> approveStore(
            @PathVariable UUID applicationId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
       StoreApplicationProcessResponse response = adminStoreService.approveStore(applicationId, userDetails);

       return ResponseEntity.ok(ApiResponse.success("가게 등록 승인 성공", response));

    }

    /**
     * 가게 등록 신청 거절
     */
    @PatchMapping("/applications/{applicationId}/reject")
    public ResponseEntity<ApiResponse<StoreApplicationProcessResponse>> rejectStore(
            @PathVariable UUID applicationId,
            @Valid @RequestBody StoreRejectRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        StoreApplicationProcessResponse response = adminStoreService.rejectStore(applicationId, request, userDetails);
        return ResponseEntity.ok(ApiResponse.success("가게 등록 거절 성공", response));
    }



    /**
     * 승인된 가게 목록 조회
     *
     * 승인된 가게의 영업 상태별로 조회 가능
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminStoreListResponse>>> getStores(
            @RequestParam(required = false) StoreOperationStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ){
        PageResponse<AdminStoreListResponse> response =
                adminStoreService.getStores(
                        status,
                        pageable
                );
        return ResponseEntity.ok(ApiResponse.success("관리 중인 가게 목록 조회 성공",  response));
    }


    /**
     * 가게 상세 조회
     */
    @GetMapping("/{storeId}")
    public ResponseEntity<ApiResponse<AdminStoreDetailResponse>> getStore(
            @PathVariable UUID storeId
    ){
        AdminStoreDetailResponse response = adminStoreService.getStore(storeId);
        return ResponseEntity.ok(ApiResponse.success("관리 중인 가게 상세 조회 성공", response));
    }


    /**
     * 가게 삭제 승인
     */
    @DeleteMapping("/{storeId}/close-approve")
    public ResponseEntity<ApiResponse<Void>> deleteStore(
            @PathVariable UUID storeId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        adminStoreService.approveDeleteStore(storeId, userDetails);
        return ResponseEntity.ok(ApiResponse.success("가게 삭제 승인 성공"));
    }

    /**
     * 관리자 강제 삭제
     */





}
