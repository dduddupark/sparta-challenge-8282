package com.sparta.spartachallenge8282.region.presentation.dto.response;

import com.sparta.spartachallenge8282.region.domain.Region;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 지역 삭제 응답. (DELETE /api/v1/regions/{regionId})
 *
 * <p>소프트 삭제이므로 삭제 시각({@code deletedAt})을 함께 반환한다.
 */
public record RegionDeleteResponse(UUID regionId, LocalDateTime deletedAt) {
    public static RegionDeleteResponse from(Region region) {
        return new RegionDeleteResponse(region.getId(), region.getDeletedAt());
    }
}
