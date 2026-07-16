package com.sparta.spartachallenge8282.store.presentation.controller;

import com.sparta.spartachallenge8282.global.common.ApiResponse;
import com.sparta.spartachallenge8282.global.common.PageResponse;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import com.sparta.spartachallenge8282.store.application.StoreApplicationService;
import com.sparta.spartachallenge8282.store.application.StoreService;
import com.sparta.spartachallenge8282.store.domain.StoreApplicationStatus;
import com.sparta.spartachallenge8282.store.presentation.dto.request.StoreApplicationRequest;
import com.sparta.spartachallenge8282.store.presentation.dto.response.MyStoreApplicationDetailResponse;
import com.sparta.spartachallenge8282.store.presentation.dto.response.MyStoreApplicationListResponse;
import com.sparta.spartachallenge8282.store.presentation.dto.response.MyStoreApplicationCreateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/store-applications")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_OWNER')")
public class StoreApplicationController {
    private final StoreApplicationService storeApplicationService;


    /**
     * 가게 등록 신청
     *
     * CUSTOMER와 OWNER 모두 가능
     */
    @PostMapping
    public ResponseEntity<ApiResponse<MyStoreApplicationCreateResponse>> createStoreApplication(
            @Valid @RequestBody StoreApplicationRequest request,
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        MyStoreApplicationCreateResponse response = storeApplicationService.createStoreApplication(request, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("가게 등록 완료", response));

    }

    /**
     * 내 가게 등록 신청 목록 조회
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PageResponse<MyStoreApplicationListResponse>>> getMyApplications(
            @RequestParam(required = false) StoreApplicationStatus status,
            @AuthenticationPrincipal UserDetailsImpl user,
            @PageableDefault(size = 20) Pageable pageable
    ){
      PageResponse<MyStoreApplicationListResponse> response = storeApplicationService.getMyStoreApplications(user, pageable, status);
        return ResponseEntity.ok(ApiResponse.success("등록 현황 목록 조회 성공", response));
    }

    /**
     * 내 가게 등록 신청 상세 조회
     */
    @GetMapping("/my/{applicationId}")
    public ResponseEntity<ApiResponse<MyStoreApplicationDetailResponse>> getMyApplication(
            @PathVariable UUID applicationId,
            @AuthenticationPrincipal UserDetailsImpl user
    ){
        return ResponseEntity.ok(ApiResponse.success("등록 현황 상세 조회 성공", storeApplicationService.getMyStoreApplication(applicationId,user)));
    }


}
