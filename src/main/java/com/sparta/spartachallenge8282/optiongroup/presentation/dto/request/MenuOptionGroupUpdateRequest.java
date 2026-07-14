package com.sparta.spartachallenge8282.optiongroup.presentation.dto.request;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record MenuOptionGroupUpdateRequest(
        @Size(max = 100) String name,
        Boolean isRequired,
        @PositiveOrZero Integer minSelect,
        @PositiveOrZero Integer maxSelect,
        @PositiveOrZero Integer sortOrder,
        Boolean isActive
) {
}
