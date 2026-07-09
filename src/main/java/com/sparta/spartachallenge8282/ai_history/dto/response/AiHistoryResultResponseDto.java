package com.sparta.spartachallenge8282.ai_history.dto.response;

import com.sparta.spartachallenge8282.ai_history.entity.AiHistory;
import jakarta.persistence.Id;

import java.util.UUID;

public record AiHistoryResultResponseDto(

        UUID aiHistoryId,
        String response,
        boolean isSuccess

) {
    public static AiHistoryResultResponseDto from(AiHistory aiHistory) {
        return new AiHistoryResultResponseDto(
                aiHistory.getId(),
                aiHistory.getResponse(),
                aiHistory.isSuccess()
        );
    }

    ;
}