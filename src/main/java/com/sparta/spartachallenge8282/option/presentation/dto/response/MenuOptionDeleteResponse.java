package com.sparta.spartachallenge8282.option.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record MenuOptionDeleteResponse(UUID optionId, LocalDateTime deletedAt) {
}
