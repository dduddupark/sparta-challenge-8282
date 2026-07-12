package com.sparta.spartachallenge8282.ai_history.dto.response;

import com.sparta.spartachallenge8282.ai_history.entity.AiHistory;

import java.time.LocalDateTime;
import java.util.UUID;

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