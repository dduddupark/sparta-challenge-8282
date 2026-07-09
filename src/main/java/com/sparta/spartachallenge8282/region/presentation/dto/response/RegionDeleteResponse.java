package com.sparta.spartachallenge8282.region.presentation.dto.response;

import java.util.UUID;

public record RegionDeleteResponse(UUID regionId, java.time.LocalDateTime deletedAt) {
}
