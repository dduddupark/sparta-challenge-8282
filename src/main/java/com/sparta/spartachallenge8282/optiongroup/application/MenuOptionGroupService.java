package com.sparta.spartachallenge8282.optiongroup.application;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.global.common.PageableUtil;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import com.sparta.spartachallenge8282.menu.domain.Menu;
import com.sparta.spartachallenge8282.menu.domain.MenuRepository;
import com.sparta.spartachallenge8282.option.domain.MenuOptionRepository;
import com.sparta.spartachallenge8282.optiongroup.domain.MenuOptionGroup;
import com.sparta.spartachallenge8282.optiongroup.domain.MenuOptionGroupRepository;
import com.sparta.spartachallenge8282.optiongroup.presentation.dto.request.MenuOptionGroupCreateRequest;
import com.sparta.spartachallenge8282.optiongroup.presentation.dto.request.MenuOptionGroupUpdateRequest;
import com.sparta.spartachallenge8282.optiongroup.presentation.dto.response.MenuOptionGroupCreateResponse;
import com.sparta.spartachallenge8282.optiongroup.presentation.dto.response.MenuOptionGroupDeleteResponse;
import com.sparta.spartachallenge8282.optiongroup.presentation.dto.response.MenuOptionGroupResponse;
import com.sparta.spartachallenge8282.store.domain.StoreRepository;
import com.sparta.spartachallenge8282.user.domain.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 옵션 그룹 비즈니스 로직.
 *
 * <p>옵션 그룹은 메뉴에 종속 — 생성 시 상위 menu 존재를 검증한다(MENU_NOT_FOUND).
 * min/max 선택 범위는 Service(INVALID_OPTION_SELECT_RANGE) + DB @Check 로 방어.
 * OWNER 쓰기 요청은 상위 메뉴의 가게 소유권을 확인한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuOptionGroupService {

    private final MenuOptionGroupRepository optionGroupRepository;
    private final MenuRepository menuRepository;
    private final MenuOptionRepository optionRepository;
    private final StoreRepository storeRepository;

    private static final String ROLE_OWNER = UserRole.OWNER.getAuthority();
    private static final String ROLE_MANAGER = UserRole.MANAGER.getAuthority();
    private static final String ROLE_MASTER = UserRole.MASTER.getAuthority();

    /**
     * 옵션 그룹 생성.
     *
     * <p>상위 메뉴가 없으면 {@code MENU_NOT_FOUND}.
     *
     * <p>선택 범위는 엔티티 기본값을 적용한 뒤 검증한다.
     * 요청값만 검증하면 미입력 필드에 적용되는 기본값을 반영할 수 없다.
     *
     * @param menuId 옵션 그룹이 속할 메뉴 ID (경로 변수)
     */
    @Transactional
    public MenuOptionGroupCreateResponse createOptionGroup(
            UUID menuId,
            MenuOptionGroupCreateRequest request,
            UserDetailsImpl user) {
        Menu menu = findActiveMenu(menuId);
        validateStoreAccess(menu.getStoreId(), user);

        MenuOptionGroup group = MenuOptionGroup.builder()
                .menuId(menuId)
                .name(request.name())
                .isRequired(request.isRequired())
                .minSelect(request.minSelect())
                .maxSelect(request.maxSelect())
                .sortOrder(request.sortOrder())
                .isActive(request.isActive())
                .build();
        validateSelectRange(group.isRequired(), group.getMinSelect(), group.getMaxSelect());

        return MenuOptionGroupCreateResponse.from(optionGroupRepository.save(group));
    }

    /** 옵션 그룹 단건 조회. (삭제되지 않은 항목이면 비활성이어도 조회된다 — 목록 조회와 필터가 다르다) */
    public MenuOptionGroupResponse getOptionGroup(UUID id) {
        return MenuOptionGroupResponse.from(findActiveGroup(id));
    }

    /**
     * 메뉴별 옵션 그룹 목록 검색. (페이지 크기는 10/30/50 만 허용)
     *
     * <p>{@code isActive} 미지정 시 활성 항목만 조회한다. 비활성 항목이 필요하면 이 값을 명시한다 —
     * 조회는 비로그인 공개라 권한으로 분기할 수 없어 파라미터로 열어둔다.
     */
    public Page<MenuOptionGroupResponse> getOptionGroupList(UUID menuId, String keyword, Boolean isActive, Pageable pageable) {
        String searchKeyword = (keyword == null) ? "" : keyword;
        Boolean activeFilter = (isActive == null) ? true : isActive;
        Pageable normalizedPageable = PageableUtil.normalize(pageable);

        return optionGroupRepository.searchOptionGroups(menuId, searchKeyword, activeFilter, normalizedPageable)
                .map(MenuOptionGroupResponse::from);
    }

    /**
     * 옵션 그룹 수정 (부분 수정 — null 인 필드는 변경하지 않는다).
     *
     * <p>선택 범위는 변경을 적용한 뒤의 값으로 검증한다.
     * 요청값만 검증하면 변경하지 않은 기존값과의 모순을 놓친다 (예: 기존 min=1, max=1 에 min 만 3 으로 변경).
     * 검증에 실패하면 트랜잭션이 롤백되므로 변경 내용은 DB 에 반영되지 않는다.
     */
    @Transactional
    public MenuOptionGroupResponse updateOptionGroup(UUID id, MenuOptionGroupUpdateRequest request, UserDetailsImpl user) {
        MenuOptionGroup group = findActiveGroup(id);
        validateStoreAccess(findStoreIdByOptionGroup(group), user);

        group.updateInfo(request.name(), request.minSelect(), request.maxSelect(), request.sortOrder());
        group.changeRequired(request.isRequired());
        group.changeActive(request.isActive());
        validateSelectRange(group.isRequired(), group.getMinSelect(), group.getMaxSelect());   // 최종 값으로 검증(실패 시 트랜잭션 롤백)

        return MenuOptionGroupResponse.from(group);
    }

    /**
     * 옵션 그룹 삭제 (소프트 삭제).
     *
     * <p>하위 옵션도 함께 소프트 삭제한다.
     *
     * @param user 삭제를 수행한 인증 사용자 ({@code userId} 는 {@code deleted_by} 에 기록)
     */
    @Transactional
    public MenuOptionGroupDeleteResponse deleteOptionGroup(UUID id, UserDetailsImpl user) {
        MenuOptionGroup group = optionGroupRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.OPTION_GROUP_NOT_FOUND));

        if (group.isDeleted()) {
            throw new CustomException(ErrorCode.ALREADY_DELETED_OPTION_GROUP);
        }
        validateStoreAccess(findStoreIdByOptionGroup(group), user);
        optionRepository.findAllByOptionGroupIdAndDeletedAtIsNull(id)
                .forEach(option -> option.softDelete(user.userId()));
        group.softDelete(user.userId());
        return MenuOptionGroupDeleteResponse.from(group);
    }

    private MenuOptionGroup findActiveGroup(UUID id) {
        return optionGroupRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new CustomException(ErrorCode.OPTION_GROUP_NOT_FOUND));
    }

    private UUID findStoreIdByOptionGroup(MenuOptionGroup group) {
        return findActiveMenu(group.getMenuId()).getStoreId();
    }

    private Menu findActiveMenu(UUID menuId) {
        return menuRepository.findByIdAndDeletedAtIsNull(menuId)
                .orElseThrow(() -> new CustomException(ErrorCode.MENU_NOT_FOUND));
    }

    /** 선택 범위 검증. 필수 그룹은 최소 1개 이상 선택되어야 한다. */
    private void validateSelectRange(boolean isRequired, int minSelect, int maxSelect) {
        if (minSelect > maxSelect) {
            throw new CustomException(ErrorCode.INVALID_OPTION_SELECT_RANGE);
        }
        if (isRequired && minSelect < 1) {
            throw new CustomException(ErrorCode.INVALID_OPTION_SELECT_RANGE);
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
            throw new CustomException(ErrorCode.NO_OPTION_GROUP_PERMISSION);
        }
    }
}
