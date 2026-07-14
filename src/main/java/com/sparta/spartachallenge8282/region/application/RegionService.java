package com.sparta.spartachallenge8282.region.application;

import com.sparta.spartachallenge8282.global.common.PageableUtil;
import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.region.domain.Region;
import com.sparta.spartachallenge8282.region.domain.RegionRepository;
import com.sparta.spartachallenge8282.region.presentation.dto.request.RegionCreateRequest;
import com.sparta.spartachallenge8282.region.presentation.dto.request.RegionUpdateRequest;
import com.sparta.spartachallenge8282.region.presentation.dto.response.RegionCreateResponse;
import com.sparta.spartachallenge8282.region.presentation.dto.response.RegionDeleteResponse;
import com.sparta.spartachallenge8282.region.presentation.dto.response.RegionResponse;
import com.sparta.spartachallenge8282.store.domain.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 지역 비즈니스 로직.
 *
 * <p>지역은 가게가 참조하는 마스터 데이터다 — 쓰기 권한은 MANAGER/MASTER 이고, 조회는 비로그인 공개다.
 *
 * <p>{@code is_active}(노출 토글)와 {@code is_service_available}(주문·배달 가능 여부)은 별개 축이다 —
 * 오픈 예정 지역은 노출은 하되 주문은 받지 않는다.
 *
 * <p>조회는 클래스 기본 {@code @Transactional(readOnly = true)}, 쓰기 메서드만 {@code @Transactional} 로 오버라이드한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionService {

    private final RegionRepository regionRepository;
    private final StoreRepository storeRepository;

    /**
     * 지역 생성.
     *
     * <p>이름 중복은 여기서 먼저 걸러 {@code DUPLICATE_REGION_NAME} 로 응답하고,
     * 동시 요청의 최종 방어는 partial unique index({@code uk_region_name_active})가 담당한다.
     */
    @Transactional
    public RegionCreateResponse createRegion(RegionCreateRequest request) {
        if (regionRepository.existsByNameAndDeletedAtIsNull(request.name())) {   // 최종 유니크 보장은 partial unique index(uk_region_name_active)
            throw new CustomException(ErrorCode.DUPLICATE_REGION_NAME);
        }

        Region region = Region.builder()
                .name(request.name())
                .sortOrder(request.sortOrder())
                .isActive(request.isActive())
                .isServiceAvailable(request.isServiceAvailable())
                .build();

        return RegionCreateResponse.from(regionRepository.save(region));
    }

    /** 지역 단건 조회. (비활성 지역은 조회 대상에서 제외한다 — {@code REGION_NOT_FOUND}) */
    public RegionResponse getRegion(UUID id) {
        Region region = regionRepository.findByIdAndDeletedAtIsNullAndIsActiveTrue(id)
                .orElseThrow(() -> new CustomException(ErrorCode.REGION_NOT_FOUND));
        return RegionResponse.from(region);
    }

    /** 지역 목록 검색. (공개 — 활성 항목만, 페이지 크기는 10/30/50 만 허용) */
    public Page<RegionResponse> getRegionList(String keyword, Pageable pageable) {
        String searchKeyword = (keyword == null) ? "" : keyword;   // keyword가 없으면 LIKE '%%'로 전체 조회되도록 빈 문자열로 넘긴다.
        Pageable normalizedPageable = PageableUtil.normalize(pageable);

        // 공개 조회는 활성 항목만 노출한다.
        // TODO(관리자 확장): 비활성 포함 전체 조회는 admin 엔드포인트에서 searchRegions에 isActive를 전달해 재사용한다.
        return regionRepository.searchRegions(searchKeyword, true, normalizedPageable)
                .map(RegionResponse::from);
    }

    /**
     * 지역 수정 (부분 수정 — null 인 필드는 변경하지 않는다).
     *
     * <p>이름 중복 검사는 "이름을 실제로 바꾸는 경우"에만 수행한다 — 수정 폼이 기존 이름을 그대로 돌려보내도
     * 자기 자신과 중복이라며 거부되지 않도록.
     *
     * <p>수정은 비활성 지역도 대상이다(다시 활성화할 수 있어야 하므로).
     */
    @Transactional
    public RegionResponse updateRegion(UUID id, RegionUpdateRequest request) {
        Region region = regionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new CustomException(ErrorCode.REGION_NOT_FOUND));

        if (request.name() != null
                && !request.name().equals(region.getName())
                && regionRepository.existsByNameAndDeletedAtIsNull(request.name())) {
            throw new CustomException(ErrorCode.DUPLICATE_REGION_NAME);
        }

        region.updateName(request.name());
        region.changeSortOrder(request.sortOrder());
        region.changeActive(request.isActive());
        region.changeServiceAvailable(request.isServiceAvailable());

        return RegionResponse.from(region);
    }

    /**
     * 지역 삭제 (소프트 삭제).
     *
     * <p>사용 중인 가게가 있으면 {@code REGION_IN_USE} 로 거부한다 — 참조 무결성은 FK 가 아니라
     * 애플리케이션이 지킨다(소프트 삭제라 DB FK 로는 걸러지지 않는다).
     *
     * @param userId 삭제를 수행한 사용자 ID ({@code deleted_by} 에 기록)
     */
    @Transactional
    public RegionDeleteResponse deleteRegion(UUID id, Long userId) {
        Region region = regionRepository.findById(id)   // 이미 삭제된 지역과 존재하지 않는 지역을 구분하기 위해 삭제 포함 조회
                .orElseThrow(() -> new CustomException(ErrorCode.REGION_NOT_FOUND));

        if (region.isDeleted()) {
            throw new CustomException(ErrorCode.ALREADY_DELETED_REGION);
        }

        if (storeRepository.existsByRegion_IdAndDeletedAtIsNull(id)) {
            throw new CustomException(ErrorCode.REGION_IN_USE);
        }

        region.softDelete(userId);
        return RegionDeleteResponse.from(region);
    }

}
