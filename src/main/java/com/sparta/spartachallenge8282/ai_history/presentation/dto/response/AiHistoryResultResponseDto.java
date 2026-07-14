package com.sparta.spartachallenge8282.ai_history.presentation.dto.response;

import com.sparta.spartachallenge8282.ai_history.domain.AiHistory;

import java.util.UUID;

/**
 * AI 메뉴 설명 생성 결과 응답 DTO.
 *
 * isSuccess=false여도 HTTP 200으로 응답한다 - AI 호출 실패가
 * 클라이언트 요청 자체의 실패는 아니기 때문이다 (response는 null로 내려감).
 */

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