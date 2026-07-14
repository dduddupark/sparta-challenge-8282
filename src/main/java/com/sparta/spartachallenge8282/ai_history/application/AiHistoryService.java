package com.sparta.spartachallenge8282.ai_history.application;

import com.sparta.spartachallenge8282.ai_history.infrastructure.GeminiClient;
import com.sparta.spartachallenge8282.ai_history.presentation.dto.request.AiHistoryCreateRequestDto;
import com.sparta.spartachallenge8282.ai_history.presentation.dto.response.AiHistoryItemResponseDto;
import com.sparta.spartachallenge8282.ai_history.presentation.dto.response.AiHistoryResultResponseDto;
import com.sparta.spartachallenge8282.ai_history.domain.AiHistory;
import com.sparta.spartachallenge8282.ai_history.domain.AiHistoryRepository;
import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.menu.domain.Menu;
import com.sparta.spartachallenge8282.menu.domain.MenuRepository;
import com.sparta.spartachallenge8282.store.domain.Store;
import com.sparta.spartachallenge8282.store.domain.StoreRepository;
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
    private final MenuRepository menuRepository;
    private final StoreRepository storeRepository;
    private final GeminiClient geminiClient;

    // 트랜잭션 안에서 외부 API를 호출하게 되면 응답이 느려질 때 DB커넥션을 계속 잡고있으므로 다른 메서드로 분리시킨다.
    public AiHistoryResultResponseDto createAiHistory(AiHistoryCreateRequestDto requestDto, Long userId) {

        Menu menu = menuRepository.findById(requestDto.menuId())
                .orElseThrow(()->new CustomException(ErrorCode.MENU_NOT_FOUND_FOR_AI));

        Store store = storeRepository.findById(menu.getStoreId())
                .orElseThrow(()-> new CustomException(ErrorCode.STORE_NOT_FOUND));

        if(!store.getOwner().getId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        String finalPrompt = buildPrompt(menu, requestDto.prompt());
        String response;
        boolean success;

        try {
            response = geminiClient.generate(finalPrompt);
            success = true;
        } catch(Exception e) {
            log.error("Gemini API 호출 실패 : {}", e.getMessage());
            response = null;
            success = false;
        }

        return saveAiHistory(requestDto.menuId(), userId, finalPrompt, response, success);
    }

    @Transactional(readOnly = true)
    public List<AiHistoryItemResponseDto> getAiHistories(UUID menuId, Pageable pageable) {
        Slice<AiHistory> slice = aiHistoryRepository.findByMenuId(menuId,pageable);

        List<AiHistoryItemResponseDto> content = slice.getContent().stream()
                .map(AiHistoryItemResponseDto::from)
                .toList();

        return content;
    }

    // DB만 저장하므로 저장부분에만 트랜잭션을 사용
    @Transactional
    public AiHistoryResultResponseDto saveAiHistory(UUID menuId, Long userId,
                                                     String prompt, String response, boolean success) {
        AiHistory aiHistory = AiHistory.builder()
                .menuId(menuId)
                .requestedBy(userId)
                .prompt(prompt)
                .response(response)
                .isSuccess(success)
                .build();

        return AiHistoryResultResponseDto.from(aiHistoryRepository.save(aiHistory));
    }

    // 자동모드 / 수동모드 -> 들어온 프롬프트 값에 따라서 분기한다. -> prompt가 null 이면 자동, 있으면 수동
    private String buildPrompt(Menu menu, String userPrompt) {
        if (userPrompt != null && !userPrompt.isBlank()) {
            return "메뉴명: " + menu.getName() + ". " + userPrompt;
        }

        return menu.getName() + "라는 메뉴가 있는데, 가격은 " + menu.getPrice() + "원이야. 이 메뉴를 소개하는 매력적인 설명을 50자 이내로 써줘.";
    }
}
