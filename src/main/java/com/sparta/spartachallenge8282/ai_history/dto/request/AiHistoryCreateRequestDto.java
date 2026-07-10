package com.sparta.spartachallenge8282.ai_history.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AiHistoryCreateRequestDto(

        @NotNull(message = "메뉴 ID는 필수입니다.")
        UUID menuId,

        @NotBlank(message = "프롬프트는 필수입니다.")
        @Size(max = 1000, message = "프롬프트는 1000자를 초과할 수 없습니다.")
        String prompt
){
}
