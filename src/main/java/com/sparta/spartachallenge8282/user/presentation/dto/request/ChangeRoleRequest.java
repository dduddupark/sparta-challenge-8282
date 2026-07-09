package com.sparta.spartachallenge8282.user.presentation.dto.request;

import com.sparta.spartachallenge8282.user.entity.UserRole;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(
        @NotNull(message = "변경할 역할은 필수 지정 정보입니다.")
        UserRole role
) {
}
