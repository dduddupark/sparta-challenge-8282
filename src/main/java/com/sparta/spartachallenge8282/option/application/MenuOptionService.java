package com.sparta.spartachallenge8282.option.application;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.global.common.PageableUtil;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import com.sparta.spartachallenge8282.menu.domain.Menu;
import com.sparta.spartachallenge8282.menu.domain.MenuRepository;
import com.sparta.spartachallenge8282.option.domain.MenuOption;
import com.sparta.spartachallenge8282.option.domain.MenuOptionRepository;
import com.sparta.spartachallenge8282.option.presentation.dto.request.MenuOptionCreateRequest;
import com.sparta.spartachallenge8282.option.presentation.dto.request.MenuOptionUpdateRequest;
import com.sparta.spartachallenge8282.option.presentation.dto.response.MenuOptionCreateResponse;
import com.sparta.spartachallenge8282.option.presentation.dto.response.MenuOptionDeleteResponse;
import com.sparta.spartachallenge8282.option.presentation.dto.response.MenuOptionResponse;
import com.sparta.spartachallenge8282.optiongroup.domain.MenuOptionGroup;
import com.sparta.spartachallenge8282.optiongroup.domain.MenuOptionGroupRepository;
import com.sparta.spartachallenge8282.store.domain.StoreRepository;
import com.sparta.spartachallenge8282.user.domain.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 옵션 비즈니스 로직.
 *
 * <p>옵션은 옵션 그룹에 종속 — 생성 시 상위 그룹 존재를 검증한다(OPTION_GROUP_NOT_FOUND).
 * additional_price 음수는 Service(INVALID_OPTION_PRICE) + DB @Check 로 방어.
 * OWNER 쓰기 요청은 상위 옵션 그룹과 메뉴를 따라 가게 소유권을 확인한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuOptionService {

    private final MenuOptionRepository optionRepository;
    private final MenuOptionGroupRepository optionGroupRepository;
    private final MenuRepository menuRepository;
    private final StoreRepository storeRepository;

    private static final String ROLE_OWNER = UserRole.OWNER.getAuthority();
    private static final String ROLE_MANAGER = UserRole.MANAGER.getAuthority();
    private static final String ROLE_MASTER = UserRole.MASTER.getAuthority();

    /**
     * 옵션 생성.
     *
     * <p>상위 옵션 그룹이 없으면 {@code OPTION_GROUP_NOT_FOUND}, 추가 금액이 음수면 {@code INVALID_OPTION_PRICE}.
     *
     * @param optionGroupId 옵션이 속할 옵션 그룹 ID (경로 변수)
     */
    @Transactional
    public MenuOptionCreateResponse createOption(
            UUID optionGroupId,
            MenuOptionCreateRequest request,
            UserDetailsImpl user) {
        MenuOptionGroup group = findActiveGroup(optionGroupId);
        validateStoreAccess(findStoreIdByOptionGroup(group), user);
        validatePrice(request.additionalPrice());

        MenuOption option = MenuOption.builder()
                .optionGroupId(optionGroupId)
                .name(request.name())
                .additionalPrice(request.additionalPrice())
                .sortOrder(request.sortOrder())
                .isActive(request.isActive())
                .build();

        return MenuOptionCreateResponse.from(optionRepository.save(option));
    }

    /** 옵션 단건 조회. (삭제되지 않은 항목이면 비활성이어도 조회된다 — 목록 조회와 필터가 다르다) */
    public MenuOptionResponse getOption(UUID id) {
        return MenuOptionResponse.from(findActiveOption(id));
    }

    /**
     * 옵션 그룹별 옵션 목록 검색. (페이지 크기는 10/30/50 만 허용)
     *
     * <p>{@code isActive} 미지정 시 활성 항목만 조회한다.
     */
    public Page<MenuOptionResponse> getOptionList(UUID optionGroupId, String keyword, Boolean isActive, Pageable pageable) {
        String searchKeyword = (keyword == null) ? "" : keyword;
        Boolean activeFilter = (isActive == null) ? true : isActive;
        Pageable normalizedPageable = PageableUtil.normalize(pageable);

        return optionRepository.searchOptions(optionGroupId, searchKeyword, activeFilter, normalizedPageable)
                .map(MenuOptionResponse::from);
    }

    /** 옵션 수정 (부분 수정 — null 인 필드는 변경하지 않는다). 추가 금액이 음수면 {@code INVALID_OPTION_PRICE}. */
    @Transactional
    public MenuOptionResponse updateOption(UUID id, MenuOptionUpdateRequest request, UserDetailsImpl user) {
        MenuOption option = findActiveOption(id);
        validateStoreAccess(findStoreIdByOption(option), user);
        validatePrice(request.additionalPrice());

        option.updateInfo(request.name(), request.additionalPrice(), request.sortOrder());
        option.changeActive(request.isActive());

        return MenuOptionResponse.from(option);
    }

    /**
     * 옵션 삭제 (소프트 삭제).
     *
     * @param user 삭제를 수행한 인증 사용자 ({@code userId} 는 {@code deleted_by} 에 기록)
     */
    @Transactional
    public MenuOptionDeleteResponse deleteOption(UUID id, UserDetailsImpl user) {
        MenuOption option = optionRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.OPTION_NOT_FOUND));

        if (option.isDeleted()) {
            throw new CustomException(ErrorCode.ALREADY_DELETED_OPTION);
        }
        validateStoreAccess(findStoreIdByOption(option), user);
        option.softDelete(user.userId());
        return MenuOptionDeleteResponse.from(option);
    }

    private MenuOption findActiveOption(UUID id) {
        return optionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new CustomException(ErrorCode.OPTION_NOT_FOUND));
    }

    private MenuOptionGroup findActiveGroup(UUID optionGroupId) {
        return optionGroupRepository.findByIdAndDeletedAtIsNull(optionGroupId)
                .orElseThrow(() -> new CustomException(ErrorCode.OPTION_GROUP_NOT_FOUND));
    }

    private UUID findStoreIdByOption(MenuOption option) {
        return findStoreIdByOptionGroup(findActiveGroup(option.getOptionGroupId()));
    }

    private UUID findStoreIdByOptionGroup(MenuOptionGroup group) {
        Menu menu = menuRepository.findByIdAndDeletedAtIsNull(group.getMenuId())
                .orElseThrow(() -> new CustomException(ErrorCode.MENU_NOT_FOUND));
        return menu.getStoreId();
    }

    private void validatePrice(Integer additionalPrice) {
        if (additionalPrice != null && additionalPrice < 0) {
            throw new CustomException(ErrorCode.INVALID_OPTION_PRICE);
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
            throw new CustomException(ErrorCode.NO_OPTION_PERMISSION);
        }
    }
}
