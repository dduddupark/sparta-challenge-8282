package com.sparta.spartachallenge8282.user.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(max = 100, message = "닉네임은 최대 100자까지 입력할 수 있습니다.")
        String nickname,

        @Size(max = 100, message = "주소는 최대 100자까지 입력할 수 있습니다.")
        String address
) {
}
