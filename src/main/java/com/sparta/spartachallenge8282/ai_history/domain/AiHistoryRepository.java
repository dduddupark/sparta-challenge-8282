package com.sparta.spartachallenge8282.ai_history.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiHistoryRepository extends JpaRepository<AiHistory,UUID> {
    // 메뉴별 AI 요청 이력 조회
    Slice<AiHistory> findByMenuId(UUID menuId, Pageable pageable);
}
