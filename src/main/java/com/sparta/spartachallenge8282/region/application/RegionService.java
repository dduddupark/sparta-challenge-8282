package com.sparta.spartachallenge8282.region.application;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.global.common.PageableUtil;
import com.sparta.spartachallenge8282.region.domain.Region;
import com.sparta.spartachallenge8282.region.domain.RegionRepository;
import com.sparta.spartachallenge8282.region.presentation.dto.request.RegionCreateRequest;
import com.sparta.spartachallenge8282.region.presentation.dto.request.RegionUpdateRequest;
import com.sparta.spartachallenge8282.region.presentation.dto.response.RegionResponse;
import com.sparta.spartachallenge8282.store.domain.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegionService {

    private final RegionRepository regionRepository;
    private final StoreRepository storeRepository;

    @Transactional
    public UUID createRegion(RegionCreateRequest request) {
        if (regionRepository.existsByNameAndDeletedAtIsNull(request.name())) {   // 최종 유니크 보장은 partial unique index(uk_region_name_active)
            throw new CustomException(ErrorCode.DUPLICATE_REGION_NAME);
        }

        Region region = Region.builder()
                .name(request.name())
                .sortOrder(request.sortOrder())
                .isActive(request.isActive())
                .isServiceAvailable(request.isServiceAvailable())
                .build();
        Region saved = regionRepository.save(region);

        return saved.getId();
    }

    public RegionResponse getRegion(UUID id) {
        Region region = regionRepository.findByIdAndDeletedAtIsNullAndIsActiveTrue(id)
                .orElseThrow(() -> new CustomException(ErrorCode.REGION_NOT_FOUND));
        return RegionResponse.from(region);
    }

    public Page<RegionResponse> getRegionList(String keyword, Pageable pageable) {
        String searchKeyword = (keyword == null) ? "" : keyword;   // keyword가 없으면 LIKE '%%'로 전체 조회되도록 빈 문자열로 넘긴다.
        Pageable normalizedPageable = PageableUtil.normalize(pageable);

        // 공개 조회는 활성 항목만 노출한다.
        // TODO(관리자 확장): 비활성 포함 전체 조회는 admin 엔드포인트에서 searchRegions에 isActive를 전달해 재사용한다.
        return regionRepository.searchRegions(searchKeyword, true, normalizedPageable)
                .map(RegionResponse::from);
    }

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

    @Transactional
    public LocalDateTime deleteRegion(UUID id, Long userId) {
        Region region = regionRepository.findById(id)   // 이미 삭제된 지역과 존재하지 않는 지역을 구분하기 위해 삭제 포함 조회
                .orElseThrow(() -> new CustomException(ErrorCode.REGION_NOT_FOUND));

        if (region.isDeleted()) {
            throw new CustomException(ErrorCode.ALREADY_DELETED_REGION);
        }

        if (storeRepository.existsByRegion_IdAndDeletedAtIsNull(id)) {
            throw new CustomException(ErrorCode.REGION_IN_USE);
        }

        region.softDelete(userId);
        return region.getDeletedAt();
    }

}
