package com.sparta.spartachallenge8282.menu.application;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.global.common.PageableUtil;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import com.sparta.spartachallenge8282.menu.domain.Menu;
import com.sparta.spartachallenge8282.menu.domain.MenuBadge;
import com.sparta.spartachallenge8282.menu.domain.MenuRepository;
import com.sparta.spartachallenge8282.menu.domain.MenuStatus;
import com.sparta.spartachallenge8282.menu.presentation.dto.response.MenuCreateResponse;
import com.sparta.spartachallenge8282.menu.presentation.dto.response.MenuDeleteResponse;
import com.sparta.spartachallenge8282.option.domain.MenuOptionRepository;
import com.sparta.spartachallenge8282.optiongroup.domain.MenuOptionGroup;
import com.sparta.spartachallenge8282.optiongroup.domain.MenuOptionGroupRepository;
import com.sparta.spartachallenge8282.menu.presentation.dto.request.MenuCreateRequest;
import com.sparta.spartachallenge8282.menu.presentation.dto.request.MenuUpdateRequest;
import com.sparta.spartachallenge8282.menu.presentation.dto.response.MenuResponse;
import com.sparta.spartachallenge8282.store.application.OwnerStoreService;
import com.sparta.spartachallenge8282.store.domain.StoreRepository;
import com.sparta.spartachallenge8282.user.domain.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 메뉴 비즈니스 로직.
 *
 * <p>조회는 클래스 기본 {@code @Transactional(readOnly = true)} 를 사용하고,
 * 쓰기 메서드만 {@code @Transactional} 로 오버라이드한다.
 *
 * <p>쓰기(생성·수정·AI 설명 적용·삭제)는 OWNER/MANAGER/MASTER 권한이 필요하다. OWNER 는 role 체크만으로는 부족하므로
 * Service 에서 가게 소유권을 확인한다.
 * 조회는 비로그인 공개이며, 공개 목록은 숨김 메뉴를 제외한다.
 *
 * <p><b>order 접점:</b> 주문 생성 시 order 도메인이 메뉴를 조회·검증하고 주문 시점 가격을 저장한다.
 * 조회 메서드 시그니처를 변경할 때 order 영향을 확인한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuService {

    private final MenuRepository menuRepository;
    private final MenuOptionGroupRepository optionGroupRepository;
    private final MenuOptionRepository optionRepository;
    private final StoreRepository storeRepository;
    private final OwnerStoreService ownerStoreService;

    private static final String ROLE_OWNER = UserRole.OWNER.getAuthority();
    private static final String ROLE_MANAGER = UserRole.MANAGER.getAuthority();
    private static final String ROLE_MASTER = UserRole.MASTER.getAuthority();

    /**
     * 메뉴 생성.
     *
     * <p>{@code isAiGenerated} 는 항상 {@code false} 로 시작한다 — {@code true} 로 가는 유일한 경로는
     * {@link #applyAiDescription(UUID, String, UserDetailsImpl) AI 설명 반영 API}다.
     *
     * @param storeId 메뉴가 속할 가게 ID (경로 변수)
     */
    @Transactional
    public MenuCreateResponse createMenu(UUID storeId, MenuCreateRequest request, UserDetailsImpl user) {
        // storeId 는 경로(PathVariable)로 받는다 — 메뉴의 소속 가게 식별자이므로 body 가 아니라 URL 계층에 둔다.
        validateStoreAccess(storeId, user);
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

        return MenuCreateResponse.from(menuRepository.save(menu));
    }

    /** 메뉴 단건 조회. (공개 — 숨김 메뉴 제외) */
    public MenuResponse getMenu(UUID id) {
        // 공개 조회는 숨김 메뉴를 노출하지 않는다.
        Menu menu = menuRepository.findByIdAndDeletedAtIsNullAndIsHiddenFalse(id)
                .orElseThrow(() -> new CustomException(ErrorCode.MENU_NOT_FOUND));
        return MenuResponse.from(menu);
    }

    /** 가게별 메뉴 목록 검색. (공개 — 숨김 메뉴 제외, 페이지 크기는 10/30/50 만 허용) */
    public Page<MenuResponse> getMenuList(UUID storeId, String keyword,
                                          MenuStatus status, MenuBadge badge, Pageable pageable) {
        String searchKeyword = (keyword == null) ? "" : keyword;   // null 이면 전체 검색(LIKE '%%')
        Pageable normalizedPageable = PageableUtil.normalize(pageable);
        // 공개 조회는 숨김 메뉴 제외(includeHidden=false). 숨김 포함 조회는 관리용 getManageMenuList 를 사용한다.
        return menuRepository.searchMenus(storeId, searchKeyword, status, badge, false, normalizedPageable)
                .map(MenuResponse::from);
    }

    /**
     * 관리용 메뉴 목록 검색 — 숨김 메뉴 포함. (GET /api/v1/stores/{storeId}/menus/manage)
     *
     * <p>공개 목록과 달리 인증 필수이며, OWNER 는 본인 가게만 조회할 수 있다
     * (MANAGER/MASTER 는 전체 가게). 권한 검증은 쓰기 경로와 동일한 {@link #validateStoreAccess} 를 재사용한다.
     */
    public Page<MenuResponse> getManageMenuList(UUID storeId, String keyword,
                                                MenuStatus status, MenuBadge badge,
                                                Pageable pageable, UserDetailsImpl user) {
        validateStoreAccess(storeId, user);

        String searchKeyword = (keyword == null) ? "" : keyword;
        Pageable normalizedPageable = PageableUtil.normalize(pageable);
        return menuRepository.searchMenus(storeId, searchKeyword, status, badge, true, normalizedPageable)
                .map(MenuResponse::from);
    }

    /**
     * 메뉴 노출 상태 변경. (PATCH /api/v1/menus/{menuId}/visibility)
     *
     * <p>숨긴 메뉴를 다시 노출할 수 있어야 하므로 숨김 포함 조회를 사용한다.
     */
    @Transactional
    public MenuResponse updateVisibility(UUID menuId, boolean hidden, UserDetailsImpl user) {
        Menu menu = menuRepository.findByIdAndDeletedAtIsNull(menuId)
                .orElseThrow(() -> new CustomException(ErrorCode.MENU_NOT_FOUND));

        validateStoreAccess(menu.getStoreId(), user);

        if (hidden) {
            menu.hide();
        } else {
            menu.show();
        }
        return MenuResponse.from(menu);   // 변경감지로 flush 되므로 save 불필요
    }

    /**
     * 메뉴 수정 (부분 수정 — null 인 필드는 변경하지 않는다).
     *
     * <p>{@code isAiGenerated} 는 요청으로 받지 않는다. 다만 설명을 다른 값으로 바꾸면 더 이상 AI 생성 설명이
     * 아니므로 엔티티가 {@code false} 로 정리한다 (같은 값 재전송이면 유지).
     */
    @Transactional
    public MenuResponse updateMenu(UUID id, MenuUpdateRequest request, UserDetailsImpl user) {
        Menu menu = menuRepository.findByIdAndDeletedAtIsNull(id)   // 조회를 가장 먼저 — 없으면 NOT_FOUND
                .orElseThrow(() -> new CustomException(ErrorCode.MENU_NOT_FOUND));

        validateStoreAccess(menu.getStoreId(), user);
        validatePrice(request.price());

        // 일반 수정에서 description이 다른 값으로 바뀌면 엔티티가 isAiGenerated=false 로 정리한다.
        menu.updateInfo(request.name(), request.description(), request.price(), request.sortOrder());
        menu.changeStatus(request.status());
        menu.changeBadge(request.badge());

        return MenuResponse.from(menu);   // 변경감지로 flush 되므로 save 불필요
    }

    /**
     * AI 생성 설명 적용. (PATCH /api/v1/menus/{menuId}/ai-description)
     *
     * <p>{@code isAiGenerated=true} 로 가는 유일한 경로다 — 생성·일반 수정 API 는 이 값을 받지 않는다.
     *
     * @param description AI 가 생성한 설명 텍스트 (빈 값 검증은 요청 DTO 의 {@code @NotBlank} 가 담당)
     */
    @Transactional
    public MenuResponse applyAiDescription(UUID menuId, String description, UserDetailsImpl user) {
        Menu menu = menuRepository.findByIdAndDeletedAtIsNull(menuId)
                .orElseThrow(() -> new CustomException(ErrorCode.MENU_NOT_FOUND));

        // isAiGenerated=true 는 AI 설명 적용 경로에서만 서버가 설정한다.
        validateStoreAccess(menu.getStoreId(), user);
        menu.applyAiDescription(description);

        return MenuResponse.from(menu);
    }

    /**
     * 메뉴 삭제 (소프트 삭제).
     *
     * <p>하위 옵션 그룹·옵션도 함께 소프트 삭제한다 — JPA cascade 는 실제 DELETE 만 전파하므로
     * 계단식 삭제를 명시적으로 수행한다(중간 실패 시 트랜잭션 롤백으로 반쪽 상태가 남지 않는다).
     *
     * @param user 삭제를 수행한 인증 사용자 ({@code userId} 는 {@code deleted_by} 에 기록)
     */
    @Transactional
    public MenuDeleteResponse deleteMenu(UUID id, UserDetailsImpl user) {
        Menu menu = menuRepository.findById(id)   // 이미 삭제된 것과 없는 것을 구분하려 삭제 포함 조회
                .orElseThrow(() -> new CustomException(ErrorCode.MENU_NOT_FOUND));

        if (menu.isDeleted()) {
            throw new CustomException(ErrorCode.ALREADY_DELETED_MENU);
        }

        UUID storeId = menu.getStoreId();

        validateStoreAccess(menu.getStoreId(), user);

        // 하위 옵션은 그룹 ID 목록으로 한 번에 조회해 삭제한다.
        List<MenuOptionGroup> groups = optionGroupRepository.findAllByMenuIdAndDeletedAtIsNull(id);
        if (!groups.isEmpty()) {
            List<UUID> groupIds = groups.stream().map(MenuOptionGroup::getId).toList();
            optionRepository.findAllByOptionGroupIdInAndDeletedAtIsNull(groupIds)
                    .forEach(option -> option.softDelete(user.userId()));
            groups.forEach(group -> group.softDelete(user.userId()));
        }

        menu.softDelete(user.userId());

        //공개된 메뉴가 없다면 가게를 preparing 상태로 되돌린다.
        ownerStoreService.refreshOperationStatusByMenus(storeId);

        return MenuDeleteResponse.from(menu);
    }

    private void validatePrice(Integer price) {
        if (price != null && price < 0) {
            throw new CustomException(ErrorCode.INVALID_MENU_PRICE);
        }
    }

    private void validateStoreAccess(UUID storeId, UserDetailsImpl user) {
        String role = user.role();
        if (!ROLE_OWNER.equals(role) && !ROLE_MANAGER.equals(role) && !ROLE_MASTER.equals(role)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        if (!storeRepository.existsByIdAndDeletedAtIsNull(storeId)) {
            throw new CustomException(ErrorCode.STORE_NOT_FOUND);
        }

        if (ROLE_OWNER.equals(role)
                && !storeRepository.existsByIdAndOwner_IdAndDeletedAtIsNull(storeId, user.userId())) {
            throw new CustomException(ErrorCode.NO_MENU_PERMISSION);
        }
    }
}
