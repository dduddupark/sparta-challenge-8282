package com.sparta.spartachallenge8282.category.presentation.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** 카테고리 부분 수정 요청. {@code name} 은 null이면 미변경, 공백만 있으면 검증 실패. */
public record CategoryUpdateRequest(
        @Size(max = 50)
        @Pattern(regexp = ".*\\S.*", message = "이름은 공백일 수 없습니다. 변경하지 않으려면 필드를 생략하세요.")
        String name,
        @PositiveOrZero Integer sortOrder,
        Boolean isActive
) {
}
