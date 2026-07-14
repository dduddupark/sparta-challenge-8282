package com.sparta.spartachallenge8282.menu.presentation.dto.response;

import com.sparta.spartachallenge8282.menu.domain.Menu;

import java.util.UUID;

/**
 * 메뉴 생성 응답. (POST /api/v1/stores/{storeId}/menus → 201 Created)
 */
public record MenuCreateResponse(UUID menuId) {
    public static MenuCreateResponse from(Menu menu) {
        return new MenuCreateResponse(menu.getId());
    }
}
