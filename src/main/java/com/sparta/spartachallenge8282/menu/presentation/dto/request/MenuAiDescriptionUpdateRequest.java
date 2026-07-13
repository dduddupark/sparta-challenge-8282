package com.sparta.spartachallenge8282.menu.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * AI가 생성한 메뉴 설명을 적용하는 전용 요청.
 *
 * <p>description만 받고, {@code isAiGenerated=true} 처리는 서버가 전용 API에서 수행한다.
 */
public record MenuAiDescriptionUpdateRequest(
        @NotBlank String description
) {
}
