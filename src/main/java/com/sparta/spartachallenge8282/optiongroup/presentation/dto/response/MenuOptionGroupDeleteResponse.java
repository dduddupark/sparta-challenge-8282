package com.sparta.spartachallenge8282.optiongroup.presentation.dto.response;

import com.sparta.spartachallenge8282.optiongroup.domain.MenuOptionGroup;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 옵션 그룹 삭제 응답. (DELETE /api/v1/option-groups/{optionGroupId})
 *
 * <p>소프트 삭제이므로 삭제 시각({@code deletedAt})을 함께 반환한다. 하위 옵션도 함께 삭제된다.
 */
public record MenuOptionGroupDeleteResponse(UUID optionGroupId, LocalDateTime deletedAt) {
    public static MenuOptionGroupDeleteResponse from(MenuOptionGroup optionGroup) {
        return new MenuOptionGroupDeleteResponse(optionGroup.getId(), optionGroup.getDeletedAt());
    }
}
