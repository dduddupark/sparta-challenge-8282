package com.sparta.spartachallenge8282.category.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record CategoryDeleteResponse(UUID categoryId, LocalDateTime deletedAt) {
}
