package com.sparta.spartachallenge8282.menu.presentation.dto.request;

import com.sparta.spartachallenge8282.menu.domain.MenuBadge;
import com.sparta.spartachallenge8282.menu.domain.MenuStatus;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** 메뉴 부분 수정 요청. {@code name} 은 null이면 미변경, 공백만 있으면 검증 실패. */
public record MenuUpdateRequest(
        @Size(max = 100)
        @Pattern(regexp = ".*\\S.*", message = "이름은 공백일 수 없습니다. 변경하지 않으려면 필드를 생략하세요.")
        String name,
        String description,
        @PositiveOrZero Integer price,
        @PositiveOrZero Integer sortOrder,
        MenuStatus status,
        MenuBadge badge
) {
}
