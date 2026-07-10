package com.sparta.spartachallenge8282.store.presentation.controller;

import com.sparta.spartachallenge8282.store.application.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/vi/store")
@RequiredArgsConstructor
public class StoreController {
    private final StoreService storeService;
}
