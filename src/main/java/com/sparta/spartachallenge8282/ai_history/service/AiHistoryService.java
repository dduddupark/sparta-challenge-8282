package com.sparta.spartachallenge8282.ai_history.service;

import com.sparta.spartachallenge8282.ai_history.dto.request.AiHistoryCreateRequestDto;
import com.sparta.spartachallenge8282.ai_history.dto.response.AiHistoryItemResponseDto;
import com.sparta.spartachallenge8282.ai_history.dto.response.AiHistoryResultResponseDto;
import com.sparta.spartachallenge8282.ai_history.entity.AiHistory;
import com.sparta.spartachallenge8282.ai_history.repository.AiHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiHistoryService {

    private final AiHistoryRepository aiHistoryRepository;
    private final RestClient restClient = RestClient.create();

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.api-url}")
    private String apiUrl;

    @Transactional
    public AiHistoryResultResponseDto createAiHistory(AiHistoryCreateRequestDto requestDto, Long userId) {
        // TODO : 메뉴 존재 여부, 소유주 검증 추가

        String response;
        boolean success;

        try {
            response = callGemini(requestDto.prompt());
            success = true;
        } catch(Exception e) {
            log.error("Gemini API 호출 실패 : {}", e.getMessage());
            response = null;
            success = false;
        }

        AiHistory aiHistory = AiHistory.builder()
                .menuId(requestDto.menuId())
                .requestedBy(userId)
                .prompt(requestDto.prompt())
                .response(response)
                .isSuccess(success)
                .build();

        return AiHistoryResultResponseDto.from(aiHistoryRepository.save(aiHistory));
    }

    @Transactional(readOnly = true)
    public List<AiHistoryItemResponseDto> getAiHistories(UUID menuId, Pageable pageable){
        Slice<AiHistory> slice = aiHistoryRepository.findByMenuId(menuId,pageable);

        List<AiHistoryItemResponseDto> content = slice.getContent().stream()
                .map(AiHistoryItemResponseDto::from)
                .toList();

        return content;
    }


    private String callGemini(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of(
                                "text",prompt)
                        ))
                )
        );

        Map<String, Object> response = restClient.post()
                .uri(apiUrl + "?key=" + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(Map.class);


        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");

        return (String) parts.get(0).get("text");
    }

}
