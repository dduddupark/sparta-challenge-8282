package com.sparta.spartachallenge8282.option.presentation.dto.response;

import com.sparta.spartachallenge8282.option.domain.MenuOption;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 옵션 삭제 응답. (DELETE /api/v1/options/{optionId})
 *
 * <p>소프트 삭제이므로 삭제 시각({@code deletedAt})을 함께 반환한다.
 */
public record MenuOptionDeleteResponse(UUID optionId, LocalDateTime deletedAt) {
    public static MenuOptionDeleteResponse from(MenuOption option) {
        return new MenuOptionDeleteResponse(option.getId(), option.getDeletedAt());
    }
}
