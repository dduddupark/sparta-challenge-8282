package com.sparta.spartachallenge8282.region.presentation;

import com.sparta.spartachallenge8282.global.common.ApiResponse;
import com.sparta.spartachallenge8282.global.common.PageResponse;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import com.sparta.spartachallenge8282.region.application.RegionService;
import com.sparta.spartachallenge8282.region.presentation.dto.request.RegionCreateRequest;
import com.sparta.spartachallenge8282.region.presentation.dto.request.RegionUpdateRequest;
import com.sparta.spartachallenge8282.region.presentation.dto.response.RegionCreateResponse;
import com.sparta.spartachallenge8282.region.presentation.dto.response.RegionDeleteResponse;
import com.sparta.spartachallenge8282.region.presentation.dto.response.RegionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

// TODO: 생성/수정/삭제는 MANAGER 전용 — 인가(@PreAuthorize) 미구현, User 연동 시 추가
@RestController
@RequestMapping("/api/v1/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    @PostMapping
    public ResponseEntity<ApiResponse<RegionCreateResponse>> createRegion(
            @Valid @RequestBody RegionCreateRequest request) {
        UUID regionId = regionService.createRegion(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("지역 생성 완료", new RegionCreateResponse(regionId)));
    }

    @GetMapping("/{regionId}")
    public ResponseEntity<ApiResponse<RegionResponse>> getRegion(@PathVariable UUID regionId) {
        return ResponseEntity.ok(
                ApiResponse.success("지역 조회 성공", regionService.getRegion(regionId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RegionResponse>>> getRegionList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 10, sort = "sortOrder") Pageable pageable) {
        PageResponse<RegionResponse> data =
                PageResponse.from(regionService.getRegionList(keyword, isActive, pageable));
        return ResponseEntity.ok(ApiResponse.success("지역 목록 조회 성공", data));
    }

    @PatchMapping("/{regionId}")
    public ResponseEntity<ApiResponse<RegionResponse>> updateRegion(
            @PathVariable UUID regionId,
            @Valid @RequestBody RegionUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("지역 수정 완료", regionService.updateRegion(regionId, request)));
    }

    @DeleteMapping("/{regionId}")
    public ResponseEntity<ApiResponse<RegionDeleteResponse>> deleteRegion(
            @PathVariable UUID regionId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        LocalDateTime deletedAt = regionService.deleteRegion(regionId, userDetails.getUserId());
        return ResponseEntity.ok(
                ApiResponse.success("지역 삭제 완료", new RegionDeleteResponse(regionId, deletedAt)));
    }
}
