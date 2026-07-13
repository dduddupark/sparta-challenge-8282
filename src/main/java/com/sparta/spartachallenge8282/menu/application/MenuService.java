package com.sparta.spartachallenge8282.menu.application;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.global.common.PageableUtil;
import com.sparta.spartachallenge8282.menu.domain.Menu;
import com.sparta.spartachallenge8282.menu.domain.MenuBadge;
import com.sparta.spartachallenge8282.menu.domain.MenuRepository;
import com.sparta.spartachallenge8282.menu.domain.MenuStatus;
import com.sparta.spartachallenge8282.menu.option.domain.MenuOptionRepository;
import com.sparta.spartachallenge8282.menu.optiongroup.domain.MenuOptionGroupRepository;
import com.sparta.spartachallenge8282.menu.presentation.dto.request.MenuCreateRequest;
import com.sparta.spartachallenge8282.menu.presentation.dto.request.MenuUpdateRequest;
import com.sparta.spartachallenge8282.menu.presentation.dto.response.MenuResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 메뉴 비즈니스 로직.
 *
 * <p>조회는 클래스 기본 {@code @Transactional(readOnly = true)}, 쓰기 메서드만 {@code @Transactional} 로 오버라이드한다.
 *
 * <p><b>권한 정책 (노션 API 명세 확정):</b>
 * <ul>
 *   <li>생성·수정·삭제: {@code OWNER}(본인 가게) 또는 {@code MANAGER}.
 *       OWNER 는 role 체크만으론 부족하고 {@code storeId} 로 가게 소유권을 확인해야 한다
 *       ({@code NO_MENU_PERMISSION}) — store 연동(auth 브랜치)에서 구현.</li>
 *   <li>단건·목록 조회: 비로그인 공개(region·category 와 동일). 공개 목록은 숨김(isHidden) 메뉴를 제외한다.</li>
 * </ul>
 *
 * <p><b>order 접점:</b> 주문 생성 시 order 도메인이 메뉴를 조회·검증한다
 * (가게 일치 {@code MENU_STORE_MISMATCH}, 숨김 {@code HIDDEN_MENU_NOT_ORDERABLE}, 가격 스냅샷).
 * order 가 이 서비스/리포지토리를 재사용할 수 있으므로 조회 시그니처 변경 시 order 영향 확인.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuService {

    private final MenuRepository menuRepository;
    private final MenuOptionGroupRepository optionGroupRepository;
    private final MenuOptionRepository optionRepository;

    @Transactional
    public UUID createMenu(UUID storeId, MenuCreateRequest request) {
        // storeId 는 경로(PathVariable)로 받는다 — 메뉴의 소속 가게 식별자이므로 body 가 아니라 URL 계층에 둔다.
        // TODO(권한, auth 브랜치): OWNER 는 storeId 가 본인 가게인지 확인 → 아니면 NO_MENU_PERMISSION.
        //                        가게 존재 검증(STORE_NOT_FOUND)도 여기서. MANAGER 는 소유권과 무관하게 허용.
        validatePrice(request.price());

        Menu menu = Menu.builder()
                .name(request.name())
                .storeId(storeId)
                .description(request.description())
                .price(request.price())
                .sortOrder(request.sortOrder())
                .status(request.status())
                .badge(request.badge())
                .build();

        return menuRepository.save(menu).getId();
    }

    public MenuResponse getMenu(UUID id) {
        // order 접점: 주문 생성 시 order 도메인이 메뉴 유효성(존재·가게 일치·숨김 여부)을 확인할 때 이 조회를 재사용할 수 있다.
        Menu menu = menuRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new CustomException(ErrorCode.MENU_NOT_FOUND));
        return MenuResponse.from(menu);
    }

    public Page<MenuResponse> getMenuList(UUID storeId, String keyword,
                                          MenuStatus status, MenuBadge badge, Pageable pageable) {
        String searchKeyword = (keyword == null) ? "" : keyword;   // null 이면 전체 검색(LIKE '%%')
        Pageable normalizedPageable = PageableUtil.normalize(pageable);
        // 공개 조회는 숨김 메뉴 제외(includeHidden=false).
        // TODO(관리자 확장): 숨김 포함 조회는 admin 엔드포인트에서 includeHidden=true 로 재사용한다.
        return menuRepository.searchMenus(storeId, searchKeyword, status, badge, false, normalizedPageable)
                .map(MenuResponse::from);
    }

    @Transactional
    public MenuResponse updateMenu(UUID id, MenuUpdateRequest request) {
        Menu menu = menuRepository.findByIdAndDeletedAtIsNull(id)   // 조회를 가장 먼저 — 없으면 NOT_FOUND
                .orElseThrow(() -> new CustomException(ErrorCode.MENU_NOT_FOUND));

        // TODO(권한, menu-auth 브랜치): OWNER 는 menu.getStoreId() 가 본인 가게인지 확인 → 아니면 NO_MENU_PERMISSION
        validatePrice(request.price());

        // 일반 수정에서 description이 다른 값으로 바뀌면 엔티티가 isAiGenerated=false 로 정리한다.
        menu.updateInfo(request.name(), request.description(), request.price(), request.sortOrder());
        menu.changeStatus(request.status());
        menu.changeBadge(request.badge());

        return MenuResponse.from(menu);   // 변경감지로 flush 되므로 save 불필요
    }

    @Transactional
    public MenuResponse applyAiDescription(UUID menuId, String description) {
        Menu menu = menuRepository.findByIdAndDeletedAtIsNull(menuId)
                .orElseThrow(() -> new CustomException(ErrorCode.MENU_NOT_FOUND));

        // isAiGenerated=true 는 AI 설명 적용 경로에서만 서버가 설정한다.
        // TODO(권한, menu-auth 브랜치): OWNER 는 menu.getStoreId() 가 본인 가게인지 확인 → 아니면 NO_MENU_PERMISSION
        menu.applyAiDescription(description);

        return MenuResponse.from(menu);
    }

    @Transactional
    public LocalDateTime deleteMenu(UUID id, Long userId) {
        Menu menu = menuRepository.findById(id)   // 이미 삭제된 것과 없는 것을 구분하려 삭제 포함 조회
                .orElseThrow(() -> new CustomException(ErrorCode.MENU_NOT_FOUND));

        if (menu.isDeleted()) {
            throw new CustomException(ErrorCode.ALREADY_DELETED_MENU);
        }

        // TODO(menu-auth 브랜치): NO_MENU_PERMISSION(가게 소유자 확인) / STORE_NOT_FOUND — store, user 연동
        optionGroupRepository.findAllByMenuIdAndDeletedAtIsNull(id)
                .forEach(group -> {
                    optionRepository.findAllByOptionGroupIdAndDeletedAtIsNull(group.getId())
                            .forEach(option -> option.softDelete(userId));
                    group.softDelete(userId);
                });
        menu.softDelete(userId);
        return menu.getDeletedAt();
    }

    private void validatePrice(Integer price) {
        if (price != null && price < 0) {
            throw new CustomException(ErrorCode.INVALID_MENU_PRICE);
        }
    }
}
