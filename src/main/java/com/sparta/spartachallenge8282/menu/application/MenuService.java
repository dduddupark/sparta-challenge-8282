package com.sparta.spartachallenge8282.menu.application;

import com.sparta.spartachallenge8282.menu.domain.MenuRepository;
import com.sparta.spartachallenge8282.menu.presentation.dto.request.MenuCreateRequest;
import com.sparta.spartachallenge8282.menu.presentation.dto.request.MenuUpdateRequest;
import com.sparta.spartachallenge8282.menu.presentation.dto.response.MenuResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 메뉴 비즈니스 로직.
 *
 * <p>조회는 클래스 기본 {@code @Transactional(readOnly = true)}, 쓰기 메서드만 {@code @Transactional} 로 오버라이드한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuService {

    private final MenuRepository menuRepository;

    @Transactional
    public UUID createMenu(MenuCreateRequest request) {
        return null;
    }

    public MenuResponse getMenu(UUID id) {
        return null;
    }

    @Transactional
    public MenuResponse updateMenu(UUID id, MenuUpdateRequest request) {
        return null;
    }

    @Transactional
    public LocalDateTime deleteMenu(UUID id, Long userId) {
        return null;
    }
}
