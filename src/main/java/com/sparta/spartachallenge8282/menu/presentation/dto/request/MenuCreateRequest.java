package com.sparta.spartachallenge8282.menu.presentation.dto.request;

import com.sparta.spartachallenge8282.menu.domain.MenuBadge;
import com.sparta.spartachallenge8282.menu.domain.MenuStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record MenuCreateRequest(
        @NotBlank @Size(max = 100) String name,
        String description,
        @NotNull @PositiveOrZero Integer price,
        @PositiveOrZero Integer sortOrder,
        MenuStatus status,
        MenuBadge badge,
        Boolean isAiGenerated
) {
}
