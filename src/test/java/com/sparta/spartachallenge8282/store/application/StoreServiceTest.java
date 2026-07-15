package com.sparta.spartachallenge8282.store.application;

import com.sparta.spartachallenge8282.category.domain.Category;
import com.sparta.spartachallenge8282.global.common.PageResponse;
import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.menu.domain.MenuRepository;
import com.sparta.spartachallenge8282.menu.domain.PreviewMenuProjection;
import com.sparta.spartachallenge8282.region.domain.Region;
import com.sparta.spartachallenge8282.store.domain.Store;
import com.sparta.spartachallenge8282.store.domain.StoreApplication;
import com.sparta.spartachallenge8282.store.domain.StoreApplicationStatus;
import com.sparta.spartachallenge8282.store.domain.StoreOperationStatus;
import com.sparta.spartachallenge8282.store.domain.StoreRepository;
import com.sparta.spartachallenge8282.store.domain.StoreSortType;
import com.sparta.spartachallenge8282.store.presentation.dto.request.StoresSearchCondition;
import com.sparta.spartachallenge8282.store.presentation.dto.response.UserStoreDetailResponse;
import com.sparta.spartachallenge8282.store.presentation.dto.response.UserStoreListResponse;
import com.sparta.spartachallenge8282.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StoreService 테스트")
class StoreServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private MenuRepository menuRepository;

    @InjectMocks
    private StoreService storeService;

    private User applicant;
    private Category category;
    private Region region;

    private UUID applicationId;
    private UUID storeId;
    private UUID categoryId;
    private UUID regionId;

    @BeforeEach
    void setUp() {
        applicant = mock(User.class);
        category = mock(Category.class);
        region = mock(Region.class);

        applicationId = UUID.randomUUID();
        storeId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        regionId = UUID.randomUUID();

        lenient().when(applicant.getId())
                .thenReturn(1L);

        lenient().when(applicant.getEmail())
                .thenReturn("owner@test.com");

        lenient().when(applicant.getNickname())
                .thenReturn("테스트 사장님");

        lenient().when(category.getId())
                .thenReturn(categoryId);

        lenient().when(category.getName())
                .thenReturn("치킨");

        lenient().when(region.getId())
                .thenReturn(regionId);

        lenient().when(region.getName())
                .thenReturn("서울 강남구");
    }

    @Nested
    @DisplayName("일반 사용자 및 비회원 가게 목록 조회")
    class GetStoresTest {

        @Test
        @DisplayName("ACTIVE 가게 목록과 대표 메뉴를 페이징 조회한다")
        void getStores_success() {
            // given
            PageRequest pageable = PageRequest.of(0, 20);

            StoresSearchCondition condition =
                    createDefaultCondition();

            Store store = createRealStore();

            Page<Store> stores = new PageImpl<>(
                    List.of(store),
                    pageable,
                    1
            );

            PreviewMenuProjection firstMenu =
                    createPreviewMenu(
                            storeId,
                            "후라이드치킨",
                            18000,
                            1
                    );

            PreviewMenuProjection secondMenu =
                    createPreviewMenu(
                            storeId,
                            "양념치킨",
                            19000,
                            2
                    );

            PreviewMenuProjection thirdMenu =
                    createPreviewMenu(
                            storeId,
                            "간장치킨",
                            19500,
                            3
                    );

            when(
                    storeRepository.searchStores(
                            condition,
                            pageable
                    )
            ).thenReturn(stores);

            when(
                    menuRepository.findTop3MenusByStoreIds(
                            List.of(storeId)
                    )
            ).thenReturn(
                    List.of(
                            firstMenu,
                            secondMenu,
                            thirdMenu
                    )
            );

            // when
            PageResponse<UserStoreListResponse> response =
                    storeService.getStores(
                            condition,
                            pageable
                    );

            // then
            assertThat(response).isNotNull();

            assertThat(response.content())
                    .hasSize(1);

            assertThat(response.page())
                    .isZero();

            assertThat(response.size())
                    .isEqualTo(20);

            assertThat(response.totalElements())
                    .isEqualTo(1);

            assertThat(response.totalPages())
                    .isEqualTo(1);

            assertThat(response.hasNext())
                    .isFalse();

            UserStoreListResponse storeResponse =
                    response.content().get(0);

            assertThat(storeResponse.storeId())
                    .isEqualTo(storeId);

            assertThat(storeResponse.storeName())
                    .isEqualTo("테스트가게");

            assertThat(storeResponse.categoryName())
                    .isEqualTo("치킨");

            assertThat(storeResponse.reviewCount())
                    .isZero();

            assertThat(storeResponse.menus())
                    .hasSize(3);

            assertThat(storeResponse.menus())
                    .extracting(menu -> menu.name())
                    .containsExactly(
                            "후라이드치킨",
                            "양념치킨",
                            "간장치킨"
                    );

            assertThat(storeResponse.menus())
                    .extracting(menu -> menu.sortOrder())
                    .containsExactly(1, 2, 3);

            assertThat(storeResponse.menus())
                    .extracting(menu -> menu.price())
                    .containsExactly(
                            18000,
                            19000,
                            19500
                    );

            verify(storeRepository).searchStores(
                    condition,
                    pageable
            );

            verify(menuRepository).findTop3MenusByStoreIds(
                    List.of(storeId)
            );
        }

        @Test
        @DisplayName("대표 메뉴가 없으면 빈 메뉴 목록으로 가게를 반환한다")
        void getStores_withoutPreviewMenus() {
            // given
            PageRequest pageable = PageRequest.of(0, 20);

            StoresSearchCondition condition =
                    createDefaultCondition();

            Store store = createRealStore();

            Page<Store> stores = new PageImpl<>(
                    List.of(store),
                    pageable,
                    1
            );

            when(
                    storeRepository.searchStores(
                            condition,
                            pageable
                    )
            ).thenReturn(stores);

            when(
                    menuRepository.findTop3MenusByStoreIds(
                            List.of(storeId)
                    )
            ).thenReturn(List.of());

            // when
            PageResponse<UserStoreListResponse> response =
                    storeService.getStores(
                            condition,
                            pageable
                    );

            // then
            assertThat(response.content())
                    .hasSize(1);

            assertThat(response.content().get(0).menus())
                    .isEmpty();

            verify(storeRepository).searchStores(
                    condition,
                    pageable
            );

            verify(menuRepository).findTop3MenusByStoreIds(
                    List.of(storeId)
            );
        }

        @Test
        @DisplayName("조회된 가게가 없으면 메뉴 조회를 실행하지 않는다")
        void getStores_empty() {
            // given
            PageRequest pageable = PageRequest.of(0, 20);

            StoresSearchCondition condition =
                    new StoresSearchCondition(
                            "존재하지 않는 가게",
                            null,
                            null,
                            null,
                            StoreSortType.LATEST
                    );

            Page<Store> emptyStores =
                    Page.empty(pageable);

            when(
                    storeRepository.searchStores(
                            condition,
                            pageable
                    )
            ).thenReturn(emptyStores);

            // when
            PageResponse<UserStoreListResponse> response =
                    storeService.getStores(
                            condition,
                            pageable
                    );

            // then
            assertThat(response).isNotNull();

            assertThat(response.content())
                    .isEmpty();

            assertThat(response.totalElements())
                    .isZero();

            assertThat(response.totalPages())
                    .isZero();

            assertThat(response.hasNext())
                    .isFalse();

            verify(storeRepository).searchStores(
                    condition,
                    pageable
            );

            verify(
                    menuRepository,
                    never()
            ).findTop3MenusByStoreIds(anyList());
        }

        @Test
        @DisplayName("검색어 조건을 Repository에 전달한다")
        void getStores_withKeyword() {
            // given
            PageRequest pageable = PageRequest.of(0, 20);

            StoresSearchCondition condition =
                    new StoresSearchCondition(
                            "후라이드",
                            null,
                            null,
                            null,
                            StoreSortType.LATEST
                    );

            Store store = createRealStore();

            Page<Store> stores = new PageImpl<>(
                    List.of(store),
                    pageable,
                    1
            );

            PreviewMenuProjection previewMenu =
                    createPreviewMenu(
                            storeId,
                            "후라이드치킨",
                            18000,
                            1
                    );

            when(
                    storeRepository.searchStores(
                            condition,
                            pageable
                    )
            ).thenReturn(stores);

            when(
                    menuRepository.findTop3MenusByStoreIds(
                            List.of(storeId)
                    )
            ).thenReturn(List.of(previewMenu));

            // when
            PageResponse<UserStoreListResponse> response =
                    storeService.getStores(
                            condition,
                            pageable
                    );

            // then
            assertThat(response.content())
                    .hasSize(1);

            assertThat(response.content().get(0).menus())
                    .extracting(menu -> menu.name())
                    .containsExactly("후라이드치킨");

            verify(storeRepository).searchStores(
                    condition,
                    pageable
            );
        }

        @Test
        @DisplayName("카테고리 필터 조건을 Repository에 전달한다")
        void getStores_withCategoryFilter() {
            // given
            PageRequest pageable = PageRequest.of(0, 20);

            StoresSearchCondition condition =
                    new StoresSearchCondition(
                            null,
                            categoryId,
                            null,
                            null,
                            StoreSortType.LATEST
                    );

            Store store = createRealStore();

            Page<Store> stores = new PageImpl<>(
                    List.of(store),
                    pageable,
                    1
            );

            when(
                    storeRepository.searchStores(
                            condition,
                            pageable
                    )
            ).thenReturn(stores);

            when(
                    menuRepository.findTop3MenusByStoreIds(
                            List.of(storeId)
                    )
            ).thenReturn(List.of());

            // when
            PageResponse<UserStoreListResponse> response =
                    storeService.getStores(
                            condition,
                            pageable
                    );

            // then
            assertThat(response.content())
                    .hasSize(1);

            assertThat(response.content().get(0).categoryName())
                    .isEqualTo("치킨");

            verify(storeRepository).searchStores(
                    condition,
                    pageable
            );
        }

        @Test
        @DisplayName("지역 필터 조건을 Repository에 전달한다")
        void getStores_withRegionFilter() {
            // given
            PageRequest pageable = PageRequest.of(0, 20);

            StoresSearchCondition condition =
                    new StoresSearchCondition(
                            null,
                            null,
                            regionId,
                            null,
                            StoreSortType.LATEST
                    );

            Store store = createRealStore();

            Page<Store> stores = new PageImpl<>(
                    List.of(store),
                    pageable,
                    1
            );

            when(
                    storeRepository.searchStores(
                            condition,
                            pageable
                    )
            ).thenReturn(stores);

            when(
                    menuRepository.findTop3MenusByStoreIds(
                            List.of(storeId)
                    )
            ).thenReturn(List.of());

            // when
            PageResponse<UserStoreListResponse> response =
                    storeService.getStores(
                            condition,
                            pageable
                    );

            // then
            assertThat(response.content())
                    .hasSize(1);

            verify(storeRepository).searchStores(
                    condition,
                    pageable
            );
        }

        @Test
        @DisplayName("영업 중 필터 조건을 Repository에 전달한다")
        void getStores_withOpenFilter() {
            // given
            PageRequest pageable = PageRequest.of(0, 20);

            StoresSearchCondition condition =
                    new StoresSearchCondition(
                            null,
                            null,
                            null,
                            true,
                            StoreSortType.LATEST
                    );

            Store store = createRealStore();
            store.changeOpenStatus(true);

            Page<Store> stores = new PageImpl<>(
                    List.of(store),
                    pageable,
                    1
            );

            when(
                    storeRepository.searchStores(
                            condition,
                            pageable
                    )
            ).thenReturn(stores);

            when(
                    menuRepository.findTop3MenusByStoreIds(
                            List.of(storeId)
                    )
            ).thenReturn(List.of());

            // when
            PageResponse<UserStoreListResponse> response =
                    storeService.getStores(
                            condition,
                            pageable
                    );

            // then
            assertThat(response.content())
                    .hasSize(1);

            assertThat(response.content().get(0).isOpen())
                    .isTrue();

            verify(storeRepository).searchStores(
                    condition,
                    pageable
            );
        }

        @Test
        @DisplayName("평점 높은 순 정렬 조건을 Repository에 전달한다")
        void getStores_withRatingSort() {
            // given
            PageRequest pageable = PageRequest.of(0, 20);

            StoresSearchCondition condition =
                    new StoresSearchCondition(
                            null,
                            null,
                            null,
                            null,
                            StoreSortType.RATING_DESC
                    );

            Store store = createRealStore();

            Page<Store> stores = new PageImpl<>(
                    List.of(store),
                    pageable,
                    1
            );

            when(
                    storeRepository.searchStores(
                            condition,
                            pageable
                    )
            ).thenReturn(stores);

            when(
                    menuRepository.findTop3MenusByStoreIds(
                            List.of(storeId)
                    )
            ).thenReturn(List.of());

            // when
            PageResponse<UserStoreListResponse> response =
                    storeService.getStores(
                            condition,
                            pageable
                    );

            // then
            assertThat(response.content())
                    .hasSize(1);

            verify(storeRepository).searchStores(
                    condition,
                    pageable
            );
        }

        @Test
        @DisplayName("리뷰 많은 순 정렬 조건을 Repository에 전달한다")
        void getStores_withReviewCountSort() {
            // given
            PageRequest pageable = PageRequest.of(0, 20);

            StoresSearchCondition condition =
                    new StoresSearchCondition(
                            null,
                            null,
                            null,
                            null,
                            StoreSortType.REVIEW_COUNT_DESC
                    );

            Store store = createRealStore();

            Page<Store> stores = new PageImpl<>(
                    List.of(store),
                    pageable,
                    1
            );

            when(
                    storeRepository.searchStores(
                            condition,
                            pageable
                    )
            ).thenReturn(stores);

            when(
                    menuRepository.findTop3MenusByStoreIds(
                            List.of(storeId)
                    )
            ).thenReturn(List.of());

            // when
            PageResponse<UserStoreListResponse> response =
                    storeService.getStores(
                            condition,
                            pageable
                    );

            // then
            assertThat(response.content())
                    .hasSize(1);

            verify(storeRepository).searchStores(
                    condition,
                    pageable
            );
        }

        @Test
        @DisplayName("배달비 낮은 순 정렬 조건을 Repository에 전달한다")
        void getStores_withDeliveryFeeSort() {
            // given
            PageRequest pageable = PageRequest.of(0, 20);

            StoresSearchCondition condition =
                    new StoresSearchCondition(
                            null,
                            null,
                            null,
                            null,
                            StoreSortType.DELIVERY_FEE_ASC
                    );

            Store store = createRealStore();

            Page<Store> stores = new PageImpl<>(
                    List.of(store),
                    pageable,
                    1
            );

            when(
                    storeRepository.searchStores(
                            condition,
                            pageable
                    )
            ).thenReturn(stores);

            when(
                    menuRepository.findTop3MenusByStoreIds(
                            List.of(storeId)
                    )
            ).thenReturn(List.of());

            // when
            PageResponse<UserStoreListResponse> response =
                    storeService.getStores(
                            condition,
                            pageable
                    );

            // then
            assertThat(response.content())
                    .hasSize(1);

            verify(storeRepository).searchStores(
                    condition,
                    pageable
            );
        }

        @Test
        @DisplayName("검색어, 카테고리, 지역, 영업 여부와 정렬 조건을 함께 전달한다")
        void getStores_withCombinedConditions() {
            // given
            PageRequest pageable = PageRequest.of(0, 20);

            StoresSearchCondition condition =
                    new StoresSearchCondition(
                            "후라이드",
                            categoryId,
                            regionId,
                            true,
                            StoreSortType.RATING_DESC
                    );

            Store store = createRealStore();
            store.changeOpenStatus(true);

            Page<Store> stores = new PageImpl<>(
                    List.of(store),
                    pageable,
                    1
            );

            PreviewMenuProjection previewMenu =
                    createPreviewMenu(
                            storeId,
                            "후라이드치킨",
                            18000,
                            1
                    );

            when(
                    storeRepository.searchStores(
                            condition,
                            pageable
                    )
            ).thenReturn(stores);

            when(
                    menuRepository.findTop3MenusByStoreIds(
                            List.of(storeId)
                    )
            ).thenReturn(List.of(previewMenu));

            // when
            PageResponse<UserStoreListResponse> response =
                    storeService.getStores(
                            condition,
                            pageable
                    );

            // then
            assertThat(response.content())
                    .hasSize(1);

            UserStoreListResponse storeResponse =
                    response.content().get(0);

            assertThat(storeResponse.categoryName())
                    .isEqualTo("치킨");

            assertThat(storeResponse.isOpen())
                    .isTrue();

            assertThat(storeResponse.menus())
                    .hasSize(1);

            assertThat(storeResponse.menus().get(0).name())
                    .isEqualTo("후라이드치킨");

            verify(storeRepository).searchStores(
                    condition,
                    pageable
            );

            verify(menuRepository).findTop3MenusByStoreIds(
                    List.of(storeId)
            );
        }
    }

    @Nested
    @DisplayName("일반 사용자 및 비회원 가게 상세 조회")
    class GetStoreTest {

        @Test
        @DisplayName("ACTIVE 상태인 가게 상세 정보를 조회한다")
        void getStore_success() {
            // given
            Store store = createRealStore();

            when(
                    storeRepository
                            .findByIdAndOperationStatusAndDeletedAtIsNull(
                                    storeId,
                                    StoreOperationStatus.ACTIVE
                            )
            ).thenReturn(Optional.of(store));

            // when
            UserStoreDetailResponse response =
                    storeService.getStore(storeId);

            // then
            assertThat(response).isNotNull();

            assertThat(response.storeId())
                    .isEqualTo(storeId);

            assertThat(response.storeName())
                    .isEqualTo("테스트가게");

            assertThat(response.categoryName())
                    .isEqualTo("치킨");

            assertThat(response.regionName())
                    .isEqualTo("서울 강남구");

            assertThat(response.address())
                    .isEqualTo("서울특별시 강남구 테헤란로 123");

            assertThat(response.storeTel())
                    .isEqualTo("010-1234-5678");

            assertThat(response.storeRating())
                    .isZero();

            assertThat(response.reviewCount())
                    .isZero();

            assertThat(response.minOrderPrice())
                    .isEqualTo(15000);

            assertThat(response.deliveryFee())
                    .isEqualTo(3000);

            assertThat(response.freeDeliveryAmount())
                    .isEqualTo(30000);

            assertThat(response.openTime())
                    .isEqualTo(LocalTime.of(9, 0));

            assertThat(response.closeTime())
                    .isEqualTo(LocalTime.of(22, 0));

            assertThat(response.isOpen())
                    .isFalse();

            verify(storeRepository)
                    .findByIdAndOperationStatusAndDeletedAtIsNull(
                            storeId,
                            StoreOperationStatus.ACTIVE
                    );
        }

        @Test
        @DisplayName("존재하지 않거나 ACTIVE 상태가 아닌 가게 조회 시 예외가 발생한다")
        void getStore_notFound() {
            // given
            when(
                    storeRepository
                            .findByIdAndOperationStatusAndDeletedAtIsNull(
                                    storeId,
                                    StoreOperationStatus.ACTIVE
                            )
            ).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    storeService.getStore(storeId)
            ).isInstanceOf(CustomException.class);

            verify(storeRepository)
                    .findByIdAndOperationStatusAndDeletedAtIsNull(
                            storeId,
                            StoreOperationStatus.ACTIVE
                    );
        }
    }

    private StoresSearchCondition createDefaultCondition() {
        return new StoresSearchCondition(
                null,
                null,
                null,
                null,
                StoreSortType.LATEST
        );
    }

    private PreviewMenuProjection createPreviewMenu(
            UUID storeId,
            String name,
            int price,
            int sortOrder
    ) {
        PreviewMenuProjection projection =
                mock(PreviewMenuProjection.class);

        when(projection.getStoreId())
                .thenReturn(storeId);

        when(projection.getMenuId())
                .thenReturn(UUID.randomUUID());

        when(projection.getName())
                .thenReturn(name);

        when(projection.getPrice())
                .thenReturn(price);

        when(projection.getSortOrder())
                .thenReturn(sortOrder);

        return projection;
    }

    private Store createRealStore() {
        StoreApplication application =
                mock(StoreApplication.class);

        lenient().when(application.getId())
                .thenReturn(applicationId);

        lenient().when(application.getStatus())
                .thenReturn(StoreApplicationStatus.APPROVED);

        lenient().when(application.getApplicant())
                .thenReturn(applicant);

        lenient().when(application.getCategory())
                .thenReturn(category);

        lenient().when(application.getRegion())
                .thenReturn(region);

        lenient().when(application.getStoreName())
                .thenReturn("테스트가게");

        lenient().when(application.getStoreTel())
                .thenReturn("010-1234-5678");

        lenient().when(application.getStoreImage())
                .thenReturn(null);

        lenient().when(application.getAddress())
                .thenReturn("서울특별시 강남구 테헤란로 123");

        lenient().when(application.getMinOrderPrice())
                .thenReturn(15000);

        lenient().when(application.getDeliveryFee())
                .thenReturn(3000);

        lenient().when(application.getFreeDeliveryAmount())
                .thenReturn(30000);

        lenient().when(application.getOpenTime())
                .thenReturn(LocalTime.of(9, 0));

        lenient().when(application.getCloseTime())
                .thenReturn(LocalTime.of(22, 0));

        Store store = Store.from(application);

        ReflectionTestUtils.setField(
                store,
                "id",
                storeId
        );

        ReflectionTestUtils.setField(
                store,
                "operationStatus",
                StoreOperationStatus.ACTIVE
        );

        return store;
    }
}