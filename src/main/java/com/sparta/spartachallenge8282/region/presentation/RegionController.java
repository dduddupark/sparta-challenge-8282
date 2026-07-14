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
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 지역 REST 컨트롤러.
 *
 * <p>지역은 특정 가게에 종속되지 않는 플랫폼 공통 마스터 데이터다.
 * 쓰기 요청은 MANAGER/MASTER 권한이 필요하며, 조회는 비로그인 공개다.
 */
@RestController
@RequestMapping("/api/v1/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_MASTER')")
    @PostMapping
    public ResponseEntity<ApiResponse<RegionCreateResponse>> createRegion(
            @Valid @RequestBody RegionCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("지역 생성 완료", regionService.createRegion(request)));
    }

    // 조회(GET)는 비로그인 공개 — 활성 항목만 노출(SecurityConfig 화이트리스트).
    @GetMapping("/{regionId}")
    public ResponseEntity<ApiResponse<RegionResponse>> getRegion(@PathVariable UUID regionId) {
        return ResponseEntity.ok(
                ApiResponse.success("지역 조회 성공", regionService.getRegion(regionId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RegionResponse>>> getRegionList(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<RegionResponse> data =
                PageResponse.from(regionService.getRegionList(keyword, pageable));
        return ResponseEntity.ok(ApiResponse.success("지역 목록 조회 성공", data));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_MASTER')")
    @PatchMapping("/{regionId}")
    public ResponseEntity<ApiResponse<RegionResponse>> updateRegion(
            @PathVariable UUID regionId,
            @Valid @RequestBody RegionUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("지역 수정 완료", regionService.updateRegion(regionId, request)));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_MASTER')")
    @DeleteMapping("/{regionId}")
    public ResponseEntity<ApiResponse<RegionDeleteResponse>> deleteRegion(
            @PathVariable UUID regionId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(
                ApiResponse.success("지역 삭제 완료", regionService.deleteRegion(regionId, userDetails.userId())));
    }
}
