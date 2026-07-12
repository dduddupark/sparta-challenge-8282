package com.sparta.spartachallenge8282.ai_history.controller;

import com.sparta.spartachallenge8282.ai_history.dto.request.AiHistoryCreateRequestDto;
import com.sparta.spartachallenge8282.ai_history.dto.response.AiHistoryItemResponseDto;
import com.sparta.spartachallenge8282.ai_history.dto.response.AiHistoryResultResponseDto;
import com.sparta.spartachallenge8282.ai_history.service.AiHistoryService;
import com.sparta.spartachallenge8282.global.common.ApiResponse;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class AiHistoryController {

    private final AiHistoryService aiHistoryService;

    @PostMapping("/ai/menu-description")
    public ResponseEntity<ApiResponse<AiHistoryResultResponseDto>> createAiHistory(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody AiHistoryCreateRequestDto requestDto) {
        AiHistoryResultResponseDto response = aiHistoryService.createAiHistory(requestDto, userDetails.userId());

    return ResponseEntity.ok(ApiResponse.success("AI 요청이 처리되었습니다.",response));
    }

    @GetMapping("/menus/{menuId}/ai-histories")
    public ResponseEntity<ApiResponse<List<AiHistoryItemResponseDto>>> getAiHistories(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable UUID menuId,
            @PageableDefault(size = 10, sort = "createdAt" , direction = Sort.Direction.DESC)Pageable pageable
            ) {
        List<AiHistoryItemResponseDto> responses = aiHistoryService.getAiHistories(menuId, pageable);
        return ResponseEntity.ok(ApiResponse.success("조회 성공", responses));
    }
}
