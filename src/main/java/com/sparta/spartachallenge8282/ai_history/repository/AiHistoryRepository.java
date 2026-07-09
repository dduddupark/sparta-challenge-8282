package com.sparta.spartachallenge8282.ai_history.repository;

import com.sparta.spartachallenge8282.ai_history.entity.AiHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

public interface AiHistoryRepository extends JpaRepository<AiHistory,UUID> {
    // 메뉴별 AI 요청 이력 조회
    Slice<AiHistory> findByMenuId(UUID menuId, Pageable pageable);
}
