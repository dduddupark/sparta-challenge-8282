package com.sparta.spartachallenge8282.ai_history.presentation.dto.response;

import com.sparta.spartachallenge8282.ai_history.domain.AiHistory;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AI 생성 이력 목록 조회용 응답 DTO (GET /menus/{menuId}/ai-histories).
 * 실패했던 이력도 그대로 포함된다 - isSuccess로 성공/실패를 구분할 수 있다.
 */

public record AiHistoryItemResponseDto(
        UUID aiHistoryId,
        String prompt,
        String response,
        boolean isSuccess,
        LocalDateTime createdAt
) {
    public static AiHistoryItemResponseDto from(AiHistory aiHistory) {
        return new AiHistoryItemResponseDto(
                aiHistory.getId(),
                aiHistory.getPrompt(),
                aiHistory.getResponse(),
                aiHistory.isSuccess(),
                aiHistory.getCreatedAt()
        );
    }
}