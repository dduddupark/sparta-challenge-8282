package com.sparta.spartachallenge8282.category.presentation.dto.request;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CategoryUpdateRequest(
        @Size(max = 50) String name,
        @PositiveOrZero Integer sortOrder,
        Boolean isActive
) {
}
