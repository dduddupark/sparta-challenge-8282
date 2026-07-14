package com.sparta.spartachallenge8282.store.presentation.controller;

import com.sparta.spartachallenge8282.global.common.ApiResponse;
import com.sparta.spartachallenge8282.global.common.PageResponse;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import com.sparta.spartachallenge8282.store.application.OwnerStoreService;
import com.sparta.spartachallenge8282.store.application.StoreService;
import com.sparta.spartachallenge8282.store.presentation.dto.request.StoreOpenStatusRequest;
import com.sparta.spartachallenge8282.store.presentation.dto.request.StoreUpdateRequest;
import com.sparta.spartachallenge8282.store.presentation.dto.response.OwnerStoreDetailResponse;
import com.sparta.spartachallenge8282.store.presentation.dto.response.OwnerStoreListResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/owner/stores")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_OWNER')")
public class OwnerStoreController {
    private final OwnerStoreService ownerStoreService;


    /**
     * 자신의 가게 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getMyStores(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PageableDefault(size = 20)Pageable pageable
    ){
        PageResponse<OwnerStoreListResponse> response = ownerStoreService.getMyStores(userDetails, pageable);
        return ResponseEntity.ok(ApiResponse.success("내 가게 목록 조회 성공", response)
        );
    }


    /**
     * 자신의 가게 상세 조회
     */
    @GetMapping("/{storeId}")
    public ResponseEntity<ApiResponse<OwnerStoreDetailResponse>> getMyStore(
            @PathVariable UUID storeId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        OwnerStoreDetailResponse response = ownerStoreService.getMyStore(storeId, userDetails);

        return ResponseEntity.ok(ApiResponse.success("내 가게 상세 조회 성공", response)
        );
    }

    /**
     * 가게 운영 활성화
     */
    @PatchMapping("/{storeId}/activate")
    public ResponseEntity<ApiResponse<?>> activateStore(
            @PathVariable UUID storeId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ){
        ownerStoreService.activateStore(storeId, userDetails);
        return ResponseEntity.ok(ApiResponse.success("가게 활성화 성공"));
    }


    /**
     * 영업 시작 종료
     */
    @PatchMapping("/{storeId}/open-status")
    public ResponseEntity<ApiResponse<?>> changeOpenStatus(
            @PathVariable UUID storeId,
            @Valid @RequestBody StoreOpenStatusRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ){
        ownerStoreService.changeOpenStatus(storeId, request, userDetails);
        return ResponseEntity.ok(ApiResponse.success(request.isOpen() ? "가게 영업 시작 성공" : "가게 영업 종료 성공"));
    }


    /**
     * 가게 정보 수정
     */
    @PatchMapping("/{storeId}")
    public ResponseEntity<ApiResponse<OwnerStoreDetailResponse>> updateStore(
            @PathVariable UUID storeId,
            @Valid @RequestBody StoreUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ){
        OwnerStoreDetailResponse response = ownerStoreService.updateStore(storeId, request, userDetails);
        return ResponseEntity.ok(ApiResponse.success("가게 정보 수정 성공", response));
    }


    /**
     * 가게 삭제 요청
     */
    @PatchMapping("/{storeId}/close-request")
    public ResponseEntity<ApiResponse<?>> requestDeleteStore(
            @PathVariable UUID storeId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ){
        ownerStoreService.requestDeleteStore(storeId, userDetails);
        return ResponseEntity.ok(ApiResponse.success("가게 삭제 요청 성공"));
    }



}
