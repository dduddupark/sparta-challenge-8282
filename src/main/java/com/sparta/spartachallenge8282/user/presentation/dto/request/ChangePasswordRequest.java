package com.sparta.spartachallenge8282.user.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "현재 비밀번호는 필수 입력값입니다.")
        String currentPassword,

        @NotBlank(message = "변경할 새 비밀번호는 필수 입력값입니다.")
        @Size(min = 8, max = 20, message = "새 비밀번호는 8자 이상 20자 이하이어야 합니다.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$",
                message = "새 비밀번호는 영문자, 숫자, 특수문자를 최소 1개씩 포함해야 합니다.")
        String newPassword
) {
}
