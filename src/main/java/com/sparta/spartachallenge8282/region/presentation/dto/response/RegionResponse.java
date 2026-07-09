package com.sparta.spartachallenge8282.region.presentation.dto.response;

import com.sparta.spartachallenge8282.region.domain.Region;

import java.util.UUID;

public record RegionResponse(
        UUID regionId,
        String name,
        int sortOrder,
        boolean isActive,
        boolean isServiceAvailable
) {
    public static RegionResponse from(Region region) {
        return new RegionResponse(
                region.getId(),
                region.getName(),
                region.getSortOrder(),
                region.isActive(),
                region.isServiceAvailable()
        );
    }
}
