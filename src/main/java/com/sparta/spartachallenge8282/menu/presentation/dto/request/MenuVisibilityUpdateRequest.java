package com.sparta.spartachallenge8282.menu.presentation.dto.request;

import jakarta.validation.constraints.NotNull;

/** 메뉴 노출 상태 변경 요청. */
public record MenuVisibilityUpdateRequest(
        @NotNull(message = "hidden 값은 필수입니다.") Boolean hidden
) {
}
