package com.sparta.spartachallenge8282.store.presentation.controller;

import com.sparta.spartachallenge8282.global.common.ApiResponse;
import com.sparta.spartachallenge8282.store.application.StoreService;
import com.sparta.spartachallenge8282.store.presentation.dto.response.MyStoreApplicationCreateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vi/owner/store")
@RequiredArgsConstructor
public class OwnerStoreController {
    private final StoreService storeService;
}
