package com.sparta.spartachallenge8282.user.dto.response;

public record LoginResponse(
        String accessToken,
        String refreshToken
) {
}
