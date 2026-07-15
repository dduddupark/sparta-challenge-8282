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
import com.sparta.spartachallenge8282.user.domain.UserRole;
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

/**
 * AI 메뉴 설명 생성/조회 비즈니스 로직.
 *
 * Gemini API 호출은 트랜잭션 밖에서 수행한다 - 외부 API 응답 지연 시
 * DB 커넥션을 계속 점유하는 문제를 방지하기 위함이다
 * 그래서 createAiHistory()는 트랜잭션이 없고, DB 저장을 담당하는
 * saveAiHistory()만 별도로 @Transactional을 붙인다.
 *
 * Gemini 호출이 실패해도 예외를 던지지 않고 isSuccess=false로
 * 정상 응답한다 - AI 실패가 클라이언트 요청 자체의 실패는 아니기 때문이다.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class AiHistoryService {

    private final AiHistoryRepository aiHistoryRepository;
    private final MenuRepository menuRepository;
    private final StoreRepository storeRepository;
    private final GeminiClient geminiClient;

    /**
     * AI로 메뉴 설명을 생성하고 결과를 AiHistory에 기록한다.
     *
     * menuId로 기존 메뉴를 조회해 이름/가격을 프롬프트 재료로 쓰므로,
     * 메뉴가 먼저 존재해야 호출 가능하다. 요청자가 해당 메뉴의 가게
     * 소유주인지 검증한 뒤 Gemini를 호출한다.
     *
     * 이 메서드는 트랜잭션이 없다 - Gemini 응답을 기다리는 동안
     * DB 커넥션을 점유하지 않기 위함이다. DB 저장은 saveAiHistory()에서
     * 별도 트랜잭션으로 처리한다.
     *
     * @param requestDto menuId와 선택적 프롬프트(prompt)
     * @param userId 요청자 ID (가게 소유주 검증에 사용)
     * @return 생성 결과 (성공 시 응답 텍스트, 실패 시 isSuccess=false)
     * @throws CustomException 메뉴/가게가 없거나 본인 가게가 아닌 경우
     */

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

    /**
     * 특정 메뉴의 AI 생성 이력을 최신순으로 조회한다.
     *
     * @param menuId 조회할 메뉴 ID
     * @param pageable 페이지 정보
     * @return 이력 목록 (없으면 빈 리스트, 예외 아님)
     */

    @Transactional(readOnly = true)
    public List<AiHistoryItemResponseDto> getAiHistories(UUID menuId, Pageable pageable, Long userId, UserRole role) {

        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new CustomException(ErrorCode.MENU_NOT_FOUND_FOR_AI));

        Store store = storeRepository.findById(menu.getStoreId())
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));

        boolean isOwner = store.getOwner().getId().equals(userId);
        boolean isManagerOrMaster = role == UserRole.MANAGER || role == UserRole.MASTER;

        if (!isOwner && !isManagerOrMaster) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }


        Slice<AiHistory> slice = aiHistoryRepository.findByMenuId(menuId,pageable);

        List<AiHistoryItemResponseDto> content = slice.getContent().stream()
                .map(AiHistoryItemResponseDto::from)
                .toList();

        return content;
    }

    /**
     * DB 저장만 전담하는 메서드 - createAiHistory()에서 Gemini 호출이
     * 끝난 뒤 결과(성공/실패 모두)를 여기서 저장한다.
     * 이 메서드에만 @Transactional을 붙여 DB 작업 범위를 최소화한다.
     */

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

    /**
     * AI로 메뉴 설명을 생성하고, 성공 시 메뉴에 즉시 반영까지 한다.
     * createAiHistory()와 검증/생성 로직은 동일하나, 성공 시 Menu.description도 함께 갱신한다.
     */
    public AiHistoryResultResponseDto createAiHistoryAndApply(AiHistoryCreateRequestDto requestDto, Long userId) {

        Menu menu = menuRepository.findById(requestDto.menuId())
                .orElseThrow(() -> new CustomException(ErrorCode.MENU_NOT_FOUND_FOR_AI));

        Store store = storeRepository.findById(menu.getStoreId())
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));

        if (!store.getOwner().getId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        String finalPrompt = buildPrompt(menu, requestDto.prompt());
        String response;
        boolean success;

        try {
            response = geminiClient.generate(finalPrompt);
            success = true;
        } catch (Exception e) {
            log.error("Gemini API 호출 실패 : {}", e.getMessage());
            response = null;
            success = false;
        }

        return saveAiHistoryAndApplyToMenu(requestDto.menuId(), userId, finalPrompt, response, success);
    }

    /**
     * DB 저장 + 성공 시 Menu 반영을 한 트랜잭션에서 처리하는 신규 메서드.
     * 기존 saveAiHistory()는 그대로 두고 별도로 추가한다.
     */
    @Transactional
    public AiHistoryResultResponseDto saveAiHistoryAndApplyToMenu(UUID menuId, Long userId,
                                                                  String prompt, String response, boolean success) {
        if (success) {
            Menu menu = menuRepository.findById(menuId)
                    .orElseThrow(() -> new CustomException(ErrorCode.MENU_NOT_FOUND_FOR_AI));
            menu.applyAiDescription(response);
        }

        AiHistory aiHistory = AiHistory.builder()
                .menuId(menuId)
                .requestedBy(userId)
                .prompt(prompt)
                .response(response)
                .isSuccess(success)
                .build();

        return AiHistoryResultResponseDto.from(aiHistoryRepository.save(aiHistory));
    }

    /**
     * AI 이력을 삭제한다. 로그성 데이터라 소유자 개념이 없어
     * MANAGER/MASTER만 삭제할 수 있으며, 물리 삭제(hard delete)로 처리한다.
     */
    @Transactional
    public void deleteAiHistory(UUID aiHistoryId, UserRole role) {

        if (role != UserRole.MANAGER && role != UserRole.MASTER) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        AiHistory aiHistory = aiHistoryRepository.findById(aiHistoryId)
                .orElseThrow(() -> new CustomException(ErrorCode.AI_HISTORY_NOT_FOUND));

        aiHistoryRepository.delete(aiHistory);
    }


    /**
     * 사용자 프롬프트 유무에 따라 자동/수동 모드로 분기해 최종 프롬프트를 만든다.
     *
     * prompt가 null이거나 공백이면 자동 모드 - 메뉴 이름/가격만으로
     * 매력적인 설명을 생성하도록 요청한다.
     * 값이 있으면 수동 모드 - "메뉴명: X. {userPrompt}" 형태로 결합해
     * 사용자 의도를 반영한다.
     */
    // 자동모드 / 수동모드 -> 들어온 프롬프트 값에 따라서 분기한다. -> prompt가 null 이면 자동, 있으면 수동
    private String buildPrompt(Menu menu, String userPrompt) {
        String base = (userPrompt != null && !userPrompt.isBlank())
                ? "메뉴명: " + menu.getName() + ". " + userPrompt
                : menu.getName() + "라는 메뉴가 있는데, 가격은 " + menu.getPrice() + "원이야. 이 메뉴를 소개하는 매력적인 설명을 써줘.";

        return base + " 답변을 최대한 간결하게 50자 이하로 작성해줘.";
    }


}
