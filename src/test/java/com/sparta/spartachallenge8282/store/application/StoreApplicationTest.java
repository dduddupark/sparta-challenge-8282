package com.sparta.spartachallenge8282.store.application;

import com.sparta.spartachallenge8282.category.domain.Category;
import com.sparta.spartachallenge8282.category.domain.CategoryRepository;
import com.sparta.spartachallenge8282.global.common.PageResponse;
import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import com.sparta.spartachallenge8282.region.domain.Region;
import com.sparta.spartachallenge8282.region.domain.RegionRepository;
import com.sparta.spartachallenge8282.store.domain.Store;
import com.sparta.spartachallenge8282.store.domain.StoreRepository;
import com.sparta.spartachallenge8282.store.domain.StoreStatus;
import com.sparta.spartachallenge8282.store.presentation.dto.request.StoreApplicationRequest;
import com.sparta.spartachallenge8282.store.presentation.dto.request.StoreRejectRequest;
import com.sparta.spartachallenge8282.store.presentation.dto.response.MyStoreApplicationCreateResponse;
import com.sparta.spartachallenge8282.store.presentation.dto.response.StoreApplicationListResponse;
import com.sparta.spartachallenge8282.store.presentation.dto.response.StoreApplicationProcessResponse;
import com.sparta.spartachallenge8282.user.entity.User;
import com.sparta.spartachallenge8282.user.entity.UserRole;
import com.sparta.spartachallenge8282.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @InjectMocks
    private StoreService storeService;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private RegionRepository regionRepository;


    @Test
    @DisplayName("CUSTOMER는 가게 등록 신청을 할 수 있다")
    void createStore_success() {
        // given
        Long userId = 1L;
        UUID categoryId = UUID.randomUUID();
        UUID regionId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();

        User user = createUser(userId, UserRole.CUSTOMER);
        Category category = createCategory(categoryId);
        Region region = createRegion(regionId);

        StoreApplicationRequest request =
                new StoreApplicationRequest(
                        categoryId,
                        regionId,
                        "테스트가게",
                        "010-1234-5678",
                        null,
                        "서울특별시 강남구",
                        15000,
                        3000,
                        30000,
                        LocalTime.of(9, 0),
                        LocalTime.of(22, 0)
                );

        UserDetailsImpl userDetails =
                new UserDetailsImpl(
                        userId,
                        "customer@test.com",
                        UserRole.CUSTOMER.getAuthority()
                );

        when(categoryRepository.findById(categoryId))
                .thenReturn(Optional.of(category));
        when(regionRepository.findById(regionId))
                .thenReturn(Optional.of(region));
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));
        when(storeRepository.save(any(Store.class)))
                .thenAnswer(invocation -> {
                    Store store = invocation.getArgument(0);
                    ReflectionTestUtils.setField(store, "id", storeId);
                    return store;
                });

        // when
        MyStoreApplicationCreateResponse response =
                storeService.createStore(request, userDetails);

        // then
        assertThat(response.storeName()).isEqualTo("테스트가게");
        assertThat(response.storeStatus()).isEqualTo(StoreStatus.PENDING);

        verify(storeRepository).save(any(Store.class));
    }
    @Test
    @DisplayName("사용자는 본인이 등록한 가게 목록을 조회할 수 있다")
    void getMyStoreApplications_success() {

        Long userId = 1L;

        User owner = createUser(userId, UserRole.CUSTOMER);

        Store store = createStore(
                UUID.randomUUID(),
                owner,
                createCategory(UUID.randomUUID()),
                createRegion(UUID.randomUUID()),
                "테스트가게"
        );

        Page<Store> page = new PageImpl<>(
                List.of(store),
                PageRequest.of(0,20),
                1
        );

        UserDetailsImpl userDetails =
                new UserDetailsImpl(
                        userId,
                        "customer@test.com",
                        UserRole.CUSTOMER.getAuthority()
                );

        when(storeRepository.findAllByOwner_Id(userId, PageRequest.of(0,20)))
                .thenReturn(page);

        PageResponse<StoreApplicationListResponse> response =
                storeService.getMyStoreApplications(
                        userDetails,
                        PageRequest.of(0,20)
                );

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).storeName())
                .isEqualTo("테스트가게");
    }

    @Test
    @DisplayName("관리자는 가게를 승인할 수 있다")
    void approveStore_success() {

        User owner = createUser(1L, UserRole.CUSTOMER);

        Store store = createStore(
                UUID.randomUUID(),
                owner,
                createCategory(UUID.randomUUID()),
                createRegion(UUID.randomUUID()),
                "테스트가게"
        );

        UserDetailsImpl manager =
                new UserDetailsImpl(
                        2L,
                        "manager@test.com",
                        UserRole.MANAGER.getAuthority()
                );

        when(storeRepository.findById(store.getId()))
                .thenReturn(Optional.of(store));

        StoreApplicationProcessResponse response =
                storeService.approveStore(
                        store.getId(),
                        manager
                );

        assertThat(response.storeStatus())
                .isEqualTo(StoreStatus.APPROVED);

        assertThat(owner.getRole())
                .isEqualTo(UserRole.OWNER);

        assertThat(response.storeName())
                .isEqualTo("테스트가게");

        assertThat(store.getApprovedAt())
                .isNotNull();

        assertThat(store.getRejectedAt())
                .isNull();
    }

    @Test
    @DisplayName("관리자는 가게를 거절할 수 있다")
    void rejectStore_success() {

        User owner = createUser(1L, UserRole.OWNER);

        Store store = createStore(
                UUID.randomUUID(),
                owner,
                createCategory(UUID.randomUUID()),
                createRegion(UUID.randomUUID()),
                "테스트가게"
        );

        UserDetailsImpl manager =
                new UserDetailsImpl(
                        2L,
                        "manager@test.com",
                        UserRole.MANAGER.getAuthority()
                );

        StoreRejectRequest request =
                new StoreRejectRequest("사업자등록증 확인 불가");

        when(storeRepository.findById(store.getId()))
                .thenReturn(Optional.of(store));

        StoreApplicationProcessResponse response =
                storeService.rejectStore(
                        store.getId(),
                        request,
                        manager
                );

        assertThat(response.storeStatus())
                .isEqualTo(StoreStatus.REJECTED);

        assertThat(response.rejectionReason())
                .isEqualTo("사업자등록증 확인 불가");

        assertThat(store.getRejectedAt())
                .isNotNull();

        assertThat(store.getApprovedAt())
                .isNull();

        assertThat(owner.getRole())
                .isEqualTo(UserRole.OWNER);
    }

    @Test
    @DisplayName("CUSTOMER는 승인할 수 없다")
    void approveStore_accessDenied() {

        UserDetailsImpl customer =
                new UserDetailsImpl(
                        1L,
                        "customer@test.com",
                        UserRole.CUSTOMER.getAuthority()
                );

        assertThatThrownBy(() ->
                storeService.approveStore(
                        UUID.randomUUID(),
                        customer
                ))
                .isInstanceOf(CustomException.class);

        verify(storeRepository, never())
                .findById(any());
    }





    private User createUser(
            Long userId,
            UserRole role
    ) {
        User user = User.builder()
                .email("customer@test.com")
                .password("encoded-password")
                .nickname("테스트사용자")
                .address("서울특별시 강남구")
                .role(role)
                .build();

        ReflectionTestUtils.setField(
                user,
                "id",
                userId
        );

        return user;
    }

    private Category createCategory(UUID categoryId) {
        Category category = Category.builder()
                .name("테스트카테고리")
                .sortOrder(1)
                .isActive(true)
                .build();

        ReflectionTestUtils.setField(
                category,
                "id",
                categoryId
        );

        return category;
    }

    private Region createRegion(UUID regionId) {
        Region region = Region.builder()
                .name("테스트지역")
                .sortOrder(1)
                .isActive(true)
                .isServiceAvailable(true)
                .build();

        ReflectionTestUtils.setField(
                region,
                "id",
                regionId
        );

        return region;
    }

    private Store createStore(
            UUID storeId,
            User owner,
            Category category,
            Region region,
            String storeName
    ) {
        Store store = Store.builder()
                .owner(owner)
                .category(category)
                .region(region)
                .storeName(storeName)
                .storeTel("010-1234-5678")
                .storeImage(null)
                .address("서울특별시 강남구")
                .minOrderPrice(15000)
                .deliveryFee(3000)
                .freeDeliveryAmount(30000)
                .openTime(LocalTime.of(9, 0))
                .closeTime(LocalTime.of(22, 0))
                .build();

        ReflectionTestUtils.setField(
                store,
                "id",
                storeId
        );

        return store;
    }
}