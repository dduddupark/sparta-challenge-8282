package com.sparta.spartachallenge8282.menu.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record MenuDeleteResponse(UUID menuId, LocalDateTime deletedAt) {
}
