package com.sparta.spartachallenge8282.region.presentation.dto.request;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record RegionUpdateRequest(
        @Size(max = 50) String name,
        @PositiveOrZero Integer sortOrder,
        Boolean isActive,
        Boolean isServiceAvailable
) {
}
