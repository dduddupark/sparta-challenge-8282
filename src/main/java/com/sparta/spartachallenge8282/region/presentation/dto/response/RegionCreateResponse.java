package com.sparta.spartachallenge8282.region.presentation.dto.response;

import com.sparta.spartachallenge8282.region.domain.Region;

import java.util.UUID;

/**
 * 지역 생성 응답. (POST /api/v1/regions → 201 Created)
 */
public record RegionCreateResponse(UUID regionId) {
    public static RegionCreateResponse from(Region region) {
        return new RegionCreateResponse(region.getId());
    }
}
