package com.sparta.spartachallenge8282.option.presentation;

import com.sparta.spartachallenge8282.global.common.ApiResponse;
import com.sparta.spartachallenge8282.global.common.PageResponse;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import com.sparta.spartachallenge8282.option.application.MenuOptionService;
import com.sparta.spartachallenge8282.option.presentation.dto.request.MenuOptionCreateRequest;
import com.sparta.spartachallenge8282.option.presentation.dto.request.MenuOptionUpdateRequest;
import com.sparta.spartachallenge8282.option.presentation.dto.response.MenuOptionCreateResponse;
import com.sparta.spartachallenge8282.option.presentation.dto.response.MenuOptionDeleteResponse;
import com.sparta.spartachallenge8282.option.presentation.dto.response.MenuOptionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 옵션 REST 컨트롤러.
 *
 * <p>옵션 생성/목록은 옵션 그룹 하위 경로를 사용하고,
 * 단건 조회/수정/삭제는 옵션 ID 기준 경로를 사용한다.
 *
 * <p>쓰기 요청은 OWNER/MANAGER/MASTER 권한이 필요하며,
 * 조회는 비로그인 공개다.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MenuOptionController {

    private final MenuOptionService optionService;

    @PreAuthorize("hasAnyAuthority('ROLE_OWNER','ROLE_MANAGER','ROLE_MASTER')")
    @PostMapping("/option-groups/{optionGroupId}/options")
    public ResponseEntity<ApiResponse<MenuOptionCreateResponse>> createOption(
            @PathVariable UUID optionGroupId,
            @Valid @RequestBody MenuOptionCreateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("옵션 생성 완료",
                        optionService.createOption(optionGroupId, request, userDetails)));
    }

    @GetMapping("/option-groups/{optionGroupId}/options")
    public ResponseEntity<ApiResponse<PageResponse<MenuOptionResponse>>> getOptionList(
            @PathVariable UUID optionGroupId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<MenuOptionResponse> data =
                PageResponse.from(optionService.getOptionList(optionGroupId, keyword, isActive, pageable));
        return ResponseEntity.ok(
                ApiResponse.success("옵션 목록 조회 성공", data));
    }

    @GetMapping("/options/{optionId}")
    public ResponseEntity<ApiResponse<MenuOptionResponse>> getOption(@PathVariable UUID optionId) {
        return ResponseEntity.ok(
                ApiResponse.success("옵션 조회 성공", optionService.getOption(optionId)));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_OWNER','ROLE_MANAGER','ROLE_MASTER')")
    @PatchMapping("/options/{optionId}")
    public ResponseEntity<ApiResponse<MenuOptionResponse>> updateOption(
            @PathVariable UUID optionId,
            @Valid @RequestBody MenuOptionUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(
                ApiResponse.success("옵션 수정 완료", optionService.updateOption(optionId, request, userDetails)));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_OWNER','ROLE_MANAGER','ROLE_MASTER')")
    @DeleteMapping("/options/{optionId}")
    public ResponseEntity<ApiResponse<MenuOptionDeleteResponse>> deleteOption(
            @PathVariable UUID optionId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(
                ApiResponse.success("옵션 삭제 완료", optionService.deleteOption(optionId, userDetails)));
    }
}
