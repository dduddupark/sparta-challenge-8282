package com.sparta.spartachallenge8282.menu.presentation.dto.response;

import com.sparta.spartachallenge8282.menu.domain.Menu;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 메뉴 삭제 응답. (DELETE /api/v1/menus/{menuId})
 *
 * <p>소프트 삭제이므로 삭제 시각({@code deletedAt})을 함께 반환한다. 하위 옵션 그룹·옵션도 함께 삭제된다.
 */
public record MenuDeleteResponse(UUID menuId, LocalDateTime deletedAt) {
    public static MenuDeleteResponse from(Menu menu) {
        return new MenuDeleteResponse(menu.getId(), menu.getDeletedAt());
    }
}
