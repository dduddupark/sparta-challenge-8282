package com.sparta.spartachallenge8282.option.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record MenuOptionCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @PositiveOrZero Integer additionalPrice,
        @PositiveOrZero Integer sortOrder,
        Boolean isActive
) {
}
