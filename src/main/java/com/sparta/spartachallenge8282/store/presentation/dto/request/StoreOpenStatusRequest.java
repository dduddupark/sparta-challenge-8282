package com.sparta.spartachallenge8282.store.presentation.dto.request;

import jakarta.validation.constraints.NotNull;

public record StoreOpenStatusRequest(
        @NotNull
        Boolean isOpen
) {
}
