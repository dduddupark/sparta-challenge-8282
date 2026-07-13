package com.sparta.spartachallenge8282.menu.optiongroup.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record MenuOptionGroupDeleteResponse(UUID optionGroupId, LocalDateTime deletedAt) {
}
