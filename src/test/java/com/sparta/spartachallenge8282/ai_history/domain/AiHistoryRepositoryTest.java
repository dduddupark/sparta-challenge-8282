package com.sparta.spartachallenge8282.ai_history.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AiHistoryRepositoryTest {

    @Autowired
    private AiHistoryRepository aiHistoryRepository;

    @Test
    @DisplayName("findByMenuId: 해당 메뉴의 이력만 조회됨")
    void findByMenuIdTest() {
        // given
        UUID menuId = UUID.randomUUID();
        UUID otherMenuId = UUID.randomUUID();

        AiHistory history1 = AiHistory.builder()
                .menuId(menuId)
                .requestedBy(1L)
                .prompt("프롬프트1")
                .response("응답1")
                .isSuccess(true)
                .build();
        AiHistory history2 = AiHistory.builder()
                .menuId(menuId)
                .requestedBy(1L)
                .prompt("프롬프트2")
                .response("응답2")
                .isSuccess(true)
                .build();
        AiHistory otherMenuHistory = AiHistory.builder()
                .menuId(otherMenuId)
                .requestedBy(1L)
                .prompt("다른 메뉴 프롬프트")
                .response("다른 메뉴 응답")
                .isSuccess(true)
                .build();

        aiHistoryRepository.save(history1);
        aiHistoryRepository.save(history2);
        aiHistoryRepository.save(otherMenuHistory);

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Slice<AiHistory> result = aiHistoryRepository.findByMenuId(menuId, pageable);
        System.out.println("결과: " + result.getContent().stream().map(AiHistory::getPrompt).toList());

        // then
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("findByMenuId: 이력 없는 메뉴는 빈 결과")
    void findByMenuIdTest_empty() {
        // given
        UUID menuId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Slice<AiHistory> result = aiHistoryRepository.findByMenuId(menuId, pageable);
        System.out.println("결과: " + result.getContent());

        // then
        assertThat(result.getContent()).isEmpty();
    }
}