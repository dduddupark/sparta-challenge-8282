package com.sparta.spartachallenge8282.ai_history.application;

import com.sparta.spartachallenge8282.ai_history.domain.AiHistory;
import com.sparta.spartachallenge8282.ai_history.domain.AiHistoryRepository;
import com.sparta.spartachallenge8282.ai_history.infrastructure.GeminiClient;
import com.sparta.spartachallenge8282.ai_history.presentation.dto.request.AiHistoryCreateRequestDto;
import com.sparta.spartachallenge8282.ai_history.presentation.dto.response.AiHistoryItemResponseDto;
import com.sparta.spartachallenge8282.ai_history.presentation.dto.response.AiHistoryResultResponseDto;
import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.menu.domain.Menu;
import com.sparta.spartachallenge8282.menu.domain.MenuRepository;
import com.sparta.spartachallenge8282.store.domain.Store;
import com.sparta.spartachallenge8282.store.domain.StoreRepository;
import com.sparta.spartachallenge8282.user.domain.User;
import com.sparta.spartachallenge8282.user.domain.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiHistoryServiceTest {

    @Mock
    private AiHistoryRepository aiHistoryRepository;
    @Mock
    private MenuRepository menuRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private GeminiClient geminiClient;

    @InjectMocks
    private AiHistoryService aiHistoryService;

    private void printException(Throwable e) {
        CustomException ex = (CustomException) e;
        System.out.println("예외 발생: " + ex.getErrorCode().getCode() + " - " + ex.getErrorCode().getMessage());
    }

    private User createOwner(Long ownerId) {
        User owner = User.builder()
                .email("owner@test.com")
                .password("encoded-pw")
                .nickname("사장님")
                .address("서울시 종로구")
                .role(UserRole.OWNER)
                .build();
        ReflectionTestUtils.setField(owner, "id", ownerId);
        return owner;
    }

    private Store createStore(User owner, UUID storeId) {
        Store store = Store.builder()
                .owner(owner)
                .build();
        ReflectionTestUtils.setField(store, "id", storeId);
        return store;
    }

    private Menu createMenu(UUID storeId) {
        Menu menu = Menu.builder()
                .storeId(storeId)
                .name("떡볶이")
                .price(8000)
                .build();
        ReflectionTestUtils.setField(menu, "id", UUID.randomUUID());
        return menu;
    }

    // ── createAiHistory ──────────────────────────────────────────

    @Test
    @DisplayName("AI 설명 생성 성공 (Gemini 성공)")
    void createAiHistoryTest_success() {
        // given
        UUID storeId = UUID.randomUUID();
        Long ownerId = 1L;

        Menu menu = createMenu(storeId);
        User owner = createOwner(ownerId);
        Store store = createStore(owner, storeId);

        AiHistoryCreateRequestDto requestDto = new AiHistoryCreateRequestDto(menu.getId(), null);

        when(menuRepository.findById(menu.getId())).thenReturn(Optional.of(menu));
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(geminiClient.generate(org.mockito.ArgumentMatchers.anyString())).thenReturn("맛있는 떡볶이입니다.");

        AiHistory savedHistory = AiHistory.builder()
                .menuId(menu.getId())
                .requestedBy(ownerId)
                .prompt("자동 생성 프롬프트")
                .response("맛있는 떡볶이입니다.")
                .isSuccess(true)
                .build();
        when(aiHistoryRepository.save(any(AiHistory.class))).thenReturn(savedHistory);

        // when
        AiHistoryResultResponseDto result = aiHistoryService.createAiHistory(requestDto, ownerId);
        System.out.println("결과: " + result);

        // then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("AI 설명 생성: Gemini 호출 실패해도 정상 응답(isSuccess=false)")
    void createAiHistoryTest_gemini_fail() {
        // given
        UUID storeId = UUID.randomUUID();
        Long ownerId = 1L;

        Menu menu = createMenu(storeId);
        User owner = createOwner(ownerId);
        Store store = createStore(owner, storeId);

        AiHistoryCreateRequestDto requestDto = new AiHistoryCreateRequestDto(menu.getId(), null);

        when(menuRepository.findById(menu.getId())).thenReturn(Optional.of(menu));
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(geminiClient.generate(org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new RuntimeException("Gemini API 호출 실패"));

        AiHistory savedHistory = AiHistory.builder()
                .menuId(menu.getId())
                .requestedBy(ownerId)
                .prompt("자동 생성 프롬프트")
                .response(null)
                .isSuccess(false)
                .build();
        when(aiHistoryRepository.save(any(AiHistory.class))).thenReturn(savedHistory);

        // when
        AiHistoryResultResponseDto result = aiHistoryService.createAiHistory(requestDto, ownerId);
        System.out.println("결과: " + result);

        // then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("AI 설명 생성 실패: 메뉴 없음")
    void createAiHistoryTest_fail_menu_not_found() {
        // given
        UUID menuId = UUID.randomUUID();
        AiHistoryCreateRequestDto requestDto = new AiHistoryCreateRequestDto(menuId, null);

        when(menuRepository.findById(menuId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> aiHistoryService.createAiHistory(requestDto, 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(this::printException);
    }

    @Test
    @DisplayName("AI 설명 생성 실패: 가게 없음")
    void createAiHistoryTest_fail_store_not_found() {
        // given
        UUID storeId = UUID.randomUUID();
        Menu menu = createMenu(storeId);
        AiHistoryCreateRequestDto requestDto = new AiHistoryCreateRequestDto(menu.getId(), null);

        when(menuRepository.findById(menu.getId())).thenReturn(Optional.of(menu));
        when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> aiHistoryService.createAiHistory(requestDto, 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(this::printException);
    }

    @Test
    @DisplayName("AI 설명 생성 실패: 본인 가게가 아님")
    void createAiHistoryTest_fail_not_owner() {
        // given
        UUID storeId = UUID.randomUUID();
        Long ownerId = 1L;
        Long otherUserId = 999L;

        Menu menu = createMenu(storeId);
        User owner = createOwner(ownerId);
        Store store = createStore(owner, storeId);

        AiHistoryCreateRequestDto requestDto = new AiHistoryCreateRequestDto(menu.getId(), null);

        when(menuRepository.findById(menu.getId())).thenReturn(Optional.of(menu));
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

        // when & then - otherUserId(가게주인 아님)가 요청
        assertThatThrownBy(() -> aiHistoryService.createAiHistory(requestDto, otherUserId))
                .isInstanceOf(CustomException.class)
                .satisfies(this::printException);
    }

    @Test
    @DisplayName("AI 히스토리 조회 성공")
    void getAiHistoriesTest() {
        // given
        UUID menuId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        AiHistory history = AiHistory.builder()
                .menuId(menuId)
                .requestedBy(1L)
                .prompt("자동 생성 프롬프트")
                .response("맛있는 메뉴입니다.")
                .isSuccess(true)
                .build();

        Slice<AiHistory> slice = new SliceImpl<>(List.of(history), pageable, false);

        when(aiHistoryRepository.findByMenuId(menuId, pageable)).thenReturn(slice);

        // when
        List<AiHistoryItemResponseDto> result = aiHistoryService.getAiHistories(menuId, pageable);
        System.out.println("결과: " + result);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("AI 히스토리 조회: 이력 없음 (빈 리스트)")
    void getAiHistoriesTest_empty() {
        // given
        UUID menuId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        Slice<AiHistory> emptySlice = new SliceImpl<>(List.of(), pageable, false);

        when(aiHistoryRepository.findByMenuId(menuId, pageable)).thenReturn(emptySlice);

        // when
        List<AiHistoryItemResponseDto> result = aiHistoryService.getAiHistories(menuId, pageable);
        System.out.println("결과: " + result);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("AI 설명 생성 및 메뉴 즉시 반영 성공")
    void createAiHistoryAndApplyTest_success() {
        // given
        UUID storeId = UUID.randomUUID();
        Long ownerId = 1L;

        Menu menu = createMenu(storeId);
        User owner = createOwner(ownerId);
        Store store = createStore(owner, storeId);

        AiHistoryCreateRequestDto requestDto = new AiHistoryCreateRequestDto(menu.getId(), null);

        // createAiHistoryAndApply에서 한 번, saveAiHistoryAndApplyToMenu에서 한 번 - 총 두 번 조회됨
        when(menuRepository.findById(menu.getId())).thenReturn(Optional.of(menu));
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(geminiClient.generate(org.mockito.ArgumentMatchers.anyString())).thenReturn("맛있는 떡볶이입니다.");

        AiHistory savedHistory = AiHistory.builder()
                .menuId(menu.getId())
                .requestedBy(ownerId)
                .prompt("자동 생성 프롬프트")
                .response("맛있는 떡볶이입니다.")
                .isSuccess(true)
                .build();
        when(aiHistoryRepository.save(any(AiHistory.class))).thenReturn(savedHistory);

        // when
        AiHistoryResultResponseDto result = aiHistoryService.createAiHistoryAndApply(requestDto, ownerId);
        System.out.println("결과: " + result);
        System.out.println("메뉴 반영 확인: description=" + menu.getDescription() + ", isAiGenerated=" + menu.isAiGenerated());

        // then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(menu.getDescription()).isEqualTo("맛있는 떡볶이입니다.");
        assertThat(menu.isAiGenerated()).isTrue();

        // menuRepository.findById가 두 번 호출됐는지 확인 (createAiHistoryAndApply + saveAiHistoryAndApplyToMenu)
        org.mockito.Mockito.verify(menuRepository, org.mockito.Mockito.times(2)).findById(menu.getId());
    }

    @Test
    @DisplayName("AI 설명 생성: Gemini 호출 실패 시 메뉴는 반영되지 않음")
    void createAiHistoryAndApplyTest_gemini_fail() {
        // given
        UUID storeId = UUID.randomUUID();
        Long ownerId = 1L;

        Menu menu = createMenu(storeId);
        User owner = createOwner(ownerId);
        Store store = createStore(owner, storeId);

        String originalDescription = menu.getDescription();

        AiHistoryCreateRequestDto requestDto = new AiHistoryCreateRequestDto(menu.getId(), null);

        when(menuRepository.findById(menu.getId())).thenReturn(Optional.of(menu));
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(geminiClient.generate(org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new RuntimeException("Gemini API 호출 실패"));

        AiHistory savedHistory = AiHistory.builder()
                .menuId(menu.getId())
                .requestedBy(ownerId)
                .prompt("자동 생성 프롬프트")
                .response(null)
                .isSuccess(false)
                .build();
        when(aiHistoryRepository.save(any(AiHistory.class))).thenReturn(savedHistory);

        // when
        AiHistoryResultResponseDto result = aiHistoryService.createAiHistoryAndApply(requestDto, ownerId);
        System.out.println("결과: " + result);

        // then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(menu.getDescription()).isEqualTo(originalDescription);   // 메뉴가 그대로인지 확인
        assertThat(menu.isAiGenerated()).isFalse();

        // 실패 시엔 saveAiHistoryAndApplyToMenu 안에서 menu를 재조회하지 않으므로 findById는 1번만 호출됨
        org.mockito.Mockito.verify(menuRepository, org.mockito.Mockito.times(1)).findById(menu.getId());
    }

    @Test
    @DisplayName("AI 설명 생성 및 반영 실패: 메뉴 없음")
    void createAiHistoryAndApplyTest_fail_menu_not_found() {
        // given
        UUID menuId = UUID.randomUUID();
        AiHistoryCreateRequestDto requestDto = new AiHistoryCreateRequestDto(menuId, null);

        when(menuRepository.findById(menuId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> aiHistoryService.createAiHistoryAndApply(requestDto, 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(this::printException);
    }

    @Test
    @DisplayName("AI 설명 생성 및 반영 실패: 가게 없음")
    void createAiHistoryAndApplyTest_fail_store_not_found() {
        // given
        UUID storeId = UUID.randomUUID();
        Menu menu = createMenu(storeId);
        AiHistoryCreateRequestDto requestDto = new AiHistoryCreateRequestDto(menu.getId(), null);

        when(menuRepository.findById(menu.getId())).thenReturn(Optional.of(menu));
        when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> aiHistoryService.createAiHistoryAndApply(requestDto, 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(this::printException);
    }

    @Test
    @DisplayName("AI 설명 생성 및 반영 실패: 본인 가게가 아님")
    void createAiHistoryAndApplyTest_fail_not_owner() {
        // given
        UUID storeId = UUID.randomUUID();
        Long ownerId = 1L;
        Long otherUserId = 999L;

        Menu menu = createMenu(storeId);
        User owner = createOwner(ownerId);
        Store store = createStore(owner, storeId);

        AiHistoryCreateRequestDto requestDto = new AiHistoryCreateRequestDto(menu.getId(), null);

        when(menuRepository.findById(menu.getId())).thenReturn(Optional.of(menu));
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

        // when & then
        assertThatThrownBy(() -> aiHistoryService.createAiHistoryAndApply(requestDto, otherUserId))
                .isInstanceOf(CustomException.class)
                .satisfies(this::printException);
    }

}