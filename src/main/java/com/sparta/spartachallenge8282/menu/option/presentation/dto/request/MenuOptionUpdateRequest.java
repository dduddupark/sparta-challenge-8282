package com.sparta.spartachallenge8282.menu.option.presentation.dto.request;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record MenuOptionUpdateRequest(
        @Size(max = 100) String name,
        @PositiveOrZero Integer additionalPrice,
        @PositiveOrZero Integer sortOrder,
        Boolean isActive
) {
}
