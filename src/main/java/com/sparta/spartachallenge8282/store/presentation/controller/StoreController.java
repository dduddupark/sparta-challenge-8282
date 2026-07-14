package com.sparta.spartachallenge8282.store.presentation.controller;

import com.sparta.spartachallenge8282.global.common.ApiResponse;
import com.sparta.spartachallenge8282.global.common.PageResponse;
import com.sparta.spartachallenge8282.store.application.StoreService;
import com.sparta.spartachallenge8282.store.domain.StoreApplicationStatus;
import com.sparta.spartachallenge8282.store.presentation.dto.response.AdminStoreApplicationListResponse;
import com.sparta.spartachallenge8282.store.presentation.dto.response.UserStoreDetailResponse;
import com.sparta.spartachallenge8282.store.presentation.dto.response.UserStoreListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
public class StoreController {
    private final StoreService storeService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserStoreListResponse>>> getStores(
            @PageableDefault(size = 20) Pageable pageable
    ){
       PageResponse<UserStoreListResponse> response = storeService.getStores(pageable);
        return ResponseEntity.ok(ApiResponse.success("가게 목록 조회 성공", response));
    }

    @GetMapping("/{storeId}")
    public ResponseEntity<ApiResponse<UserStoreDetailResponse>> getStore(
            @PathVariable UUID storeId
    ){
        UserStoreDetailResponse response = storeService.getStore(storeId);
        return ResponseEntity.ok(ApiResponse.success("가게 상세 조회 성공", response));
    }

}
