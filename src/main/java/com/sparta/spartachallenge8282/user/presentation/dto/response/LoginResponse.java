package com.sparta.spartachallenge8282.user.presentation.dto.response;

public record LoginResponse(
        String accessToken,
        String refreshToken
) {
}
