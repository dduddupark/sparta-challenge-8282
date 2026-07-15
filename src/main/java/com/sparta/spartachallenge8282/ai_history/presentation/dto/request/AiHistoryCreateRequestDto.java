package com.sparta.spartachallenge8282.ai_history.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * AI 메뉴 설명 생성 요청 DTO.
 *
 * prompt는 선택값이다 - null/blank면 자동 모드(메뉴 정보만으로 생성),
 * 값이 있으면 수동 모드(사용자 의도 반영)로 분기한다 (AiHistoryService.buildPrompt 참고).
 */

public record AiHistoryCreateRequestDto(

        @NotNull(message = "메뉴 ID는 필수입니다.")
        UUID menuId,

        @Size(max = 200, message = "프롬프트는 200자를 초과할 수 없습니다.")
        String prompt
){
}
