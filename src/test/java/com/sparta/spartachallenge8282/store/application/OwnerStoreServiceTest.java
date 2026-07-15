package com.sparta.spartachallenge8282.store.application;

import com.sparta.spartachallenge8282.category.domain.Category;
import com.sparta.spartachallenge8282.category.domain.CategoryRepository;
import com.sparta.spartachallenge8282.global.common.PageResponse;
import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import com.sparta.spartachallenge8282.menu.domain.MenuRepository;
import com.sparta.spartachallenge8282.region.domain.Region;
import com.sparta.spartachallenge8282.region.domain.RegionRepository;
import com.sparta.spartachallenge8282.store.domain.Store;
import com.sparta.spartachallenge8282.store.domain.StoreOperationStatus;
import com.sparta.spartachallenge8282.store.domain.StoreRepository;
import com.sparta.spartachallenge8282.store.presentation.dto.request.StoreOpenStatusRequest;
import com.sparta.spartachallenge8282.store.presentation.dto.request.StoreUpdateRequest;
import com.sparta.spartachallenge8282.store.presentation.dto.response.OwnerStoreDetailResponse;
import com.sparta.spartachallenge8282.store.presentation.dto.response.OwnerStoreListResponse;
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

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OwnerStoreService 테스트")
class OwnerStoreServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private RegionRepository regionRepository;

    @InjectMocks
    private OwnerStoreService ownerStoreService;

    private UUID storeId;
    private UUID categoryId;
    private UUID regionId;

    private Long ownerId;
    private UserDetailsImpl ownerDetails;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        regionId = UUID.randomUUID();

        ownerId = 1L;

        ownerDetails = new UserDetailsImpl(
                ownerId,
                "owner@test.com",
                "ROLE_OWNER"
        );
    }

    @Nested
    @DisplayName("본인 가게 목록 조회")
    class GetMyStoresTest {

        @Test
        @DisplayName("본인의 가게 목록을 페이징 조회한다")
        void getMyStores_success() {
            // given
            PageRequest pageable = PageRequest.of(0, 20);
            Store store = mockStore(StoreOperationStatus.ACTIVE);

            Page<Store> stores = new PageImpl<>(
                    List.of(store),
                    pageable,
                    1
            );

            when(
                    storeRepository.findAllByOwnerIdAndDeletedAtIsNull(
                            ownerId,
                            pageable
                    )
            ).thenReturn(stores);

            // when
            PageResponse<OwnerStoreListResponse> response =
                    ownerStoreService.getMyStores(
                            ownerDetails,
                            pageable
                    );

            // then
            assertThat(response).isNotNull();

            verify(storeRepository)
                    .findAllByOwnerIdAndDeletedAtIsNull(
                            ownerId,
                            pageable
                    );
        }

        @Test
        @DisplayName("본인 소유 가게가 없으면 빈 페이지를 반환한다")
        void getMyStores_empty() {
            // given
            PageRequest pageable = PageRequest.of(0, 20);
            Page<Store> stores = Page.empty(pageable);

            when(
                    storeRepository.findAllByOwnerIdAndDeletedAtIsNull(
                            ownerId,
                            pageable
                    )
            ).thenReturn(stores);

            // when
            PageResponse<OwnerStoreListResponse> response =
                    ownerStoreService.getMyStores(
                            ownerDetails,
                            pageable
                    );

            // then
            assertThat(response).isNotNull();

            verify(storeRepository)
                    .findAllByOwnerIdAndDeletedAtIsNull(
                            ownerId,
                            pageable
                    );
        }
    }

    @Nested
    @DisplayName("본인 가게 상세 조회")
    class GetMyStoreTest {

        @Test
        @DisplayName("본인의 가게 상세 정보를 조회한다")
        void getMyStore_success() {
            // given
            Store store = mockStore(StoreOperationStatus.ACTIVE);

            when(
                    storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(
                            storeId,
                            ownerId
                    )
            ).thenReturn(Optional.of(store));

            // when
            OwnerStoreDetailResponse response =
                    ownerStoreService.getMyStore(
                            storeId,
                            ownerDetails
                    );

            // then
            assertThat(response).isNotNull();

            verify(storeRepository)
                    .findByIdAndOwner_IdAndDeletedAtIsNull(
                            storeId,
                            ownerId
                    );
        }

        @Test
        @DisplayName("존재하지 않거나 다른 사용자의 가게이면 예외가 발생한다")
        void getMyStore_notFound() {
            // given
            when(
                    storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(
                            storeId,
                            ownerId
                    )
            ).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    ownerStoreService.getMyStore(
                            storeId,
                            ownerDetails
                    )
            ).isInstanceOf(CustomException.class);

            verify(storeRepository)
                    .findByIdAndOwner_IdAndDeletedAtIsNull(
                            storeId,
                            ownerId
                    );
        }
    }

    @Nested
    @DisplayName("가게 활성화")
    class ActivateStoreTest {

        @Test
        @DisplayName("PREPARING 상태이고 메뉴가 존재하면 활성화에 성공한다")
        void activateStore_success() {
            // given
            Store store = mockStore(StoreOperationStatus.PREPARING);

            when(
                    storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(
                            storeId,
                            ownerId
                    )
            ).thenReturn(Optional.of(store));

            when(
                    menuRepository.existsByStoreIdAndDeletedAtIsNullAndIsHiddenFalse(
                            storeId
                    )
            ).thenReturn(true);

            // when
            ownerStoreService.activateStore(
                    storeId,
                    ownerDetails
            );

            // then
            verify(store).activate();

            verify(menuRepository)
                    .existsByStoreIdAndDeletedAtIsNullAndIsHiddenFalse(storeId);
        }

        @Test
        @DisplayName("가게가 존재하지 않으면 활성화할 수 없다")
        void activateStore_storeNotFound() {
            // given
            when(
                    storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(
                            storeId,
                            ownerId
                    )
            ).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    ownerStoreService.activateStore(
                            storeId,
                            ownerDetails
                    )
            ).isInstanceOf(CustomException.class);

            verifyNoInteractions(menuRepository);
        }

        @Test
        @DisplayName("PREPARING 상태가 아니면 활성화할 수 없다")
        void activateStore_notPreparing() {
            // given
            Store store = mockStore(StoreOperationStatus.ACTIVE);

            when(
                    storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(
                            storeId,
                            ownerId
                    )
            ).thenReturn(Optional.of(store));

            // when & then
            assertThatThrownBy(() ->
                    ownerStoreService.activateStore(
                            storeId,
                            ownerDetails
                    )
            ).isInstanceOf(CustomException.class);

            verify(store, never()).activate();
            verifyNoInteractions(menuRepository);
        }

        @Test
        @DisplayName("등록된 메뉴가 없으면 활성화할 수 없다")
        void activateStore_menuNotFound() {
            // given
            Store store = mockStore(StoreOperationStatus.PREPARING);

            when(
                    storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(
                            storeId,
                            ownerId
                    )
            ).thenReturn(Optional.of(store));

            when(
                    menuRepository.existsByStoreIdAndDeletedAtIsNullAndIsHiddenFalse(
                            storeId
                    )
            ).thenReturn(false);

            // when & then
            assertThatThrownBy(() ->
                    ownerStoreService.activateStore(
                            storeId,
                            ownerDetails
                    )
            ).isInstanceOf(CustomException.class);

            verify(store, never()).activate();
        }
    }

    @Nested
    @DisplayName("영업 시작 및 종료")
    class ChangeOpenStatusTest {

        @Test
        @DisplayName("ACTIVE 상태인 가게는 영업을 시작할 수 있다")
        void openStore_success() {
            // given
            Store store = mockStore(StoreOperationStatus.ACTIVE);
            StoreOpenStatusRequest request =
                    new StoreOpenStatusRequest(true);

            when(
                    storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(
                            storeId,
                            ownerId
                    )
            ).thenReturn(Optional.of(store));

            // when
            ownerStoreService.changeOpenStatus(
                    storeId,
                    request,
                    ownerDetails
            );

            // then
            verify(store).changeOpenStatus(true);
        }

        @Test
        @DisplayName("ACTIVE 상태인 가게는 영업을 종료할 수 있다")
        void closeStore_success() {
            // given
            Store store = mockStore(StoreOperationStatus.ACTIVE);
            StoreOpenStatusRequest request =
                    new StoreOpenStatusRequest(false);

            when(
                    storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(
                            storeId,
                            ownerId
                    )
            ).thenReturn(Optional.of(store));

            // when
            ownerStoreService.changeOpenStatus(
                    storeId,
                    request,
                    ownerDetails
            );

            // then
            verify(store).changeOpenStatus(false);
        }

        @Test
        @DisplayName("가게가 존재하지 않으면 영업 상태를 변경할 수 없다")
        void changeOpenStatus_storeNotFound() {
            // given
            StoreOpenStatusRequest request =
                    new StoreOpenStatusRequest(true);

            when(
                    storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(
                            storeId,
                            ownerId
                    )
            ).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    ownerStoreService.changeOpenStatus(
                            storeId,
                            request,
                            ownerDetails
                    )
            ).isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("PREPARING 상태에서는 영업 상태를 변경할 수 없다")
        void changeOpenStatus_preparing() {
            // given
            Store store = mockStore(StoreOperationStatus.PREPARING);
            StoreOpenStatusRequest request =
                    new StoreOpenStatusRequest(true);

            when(
                    storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(
                            storeId,
                            ownerId
                    )
            ).thenReturn(Optional.of(store));

            // when & then
            assertThatThrownBy(() ->
                    ownerStoreService.changeOpenStatus(
                            storeId,
                            request,
                            ownerDetails
                    )
            ).isInstanceOf(CustomException.class);

            verify(store, never())
                    .changeOpenStatus(anyBoolean());
        }

        @Test
        @DisplayName("CLOSE_REQUESTED 상태에서는 영업 상태를 변경할 수 없다")
        void changeOpenStatus_closeRequested() {
            // given
            Store store =
                    mockStore(StoreOperationStatus.CLOSE_REQUESTED);

            StoreOpenStatusRequest request =
                    new StoreOpenStatusRequest(true);

            when(
                    storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(
                            storeId,
                            ownerId
                    )
            ).thenReturn(Optional.of(store));

            // when & then
            assertThatThrownBy(() ->
                    ownerStoreService.changeOpenStatus(
                            storeId,
                            request,
                            ownerDetails
                    )
            ).isInstanceOf(CustomException.class);

            verify(store, never())
                    .changeOpenStatus(anyBoolean());
        }
    }

    @Nested
    @DisplayName("가게 정보 수정")
    class UpdateStoreTest {

        @Test
        @DisplayName("가게 정보 수정에 성공한다")
        void updateStore_success() {
            // given
            Store store = mockStore(StoreOperationStatus.ACTIVE);
            Category newCategory = mock(Category.class);
            Region newRegion = mock(Region.class);
            StoreUpdateRequest request = createUpdateRequest();

            when(
                    storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(
                            storeId,
                            ownerId
                    )
            ).thenReturn(Optional.of(store));

            when(categoryRepository.findById(categoryId))
                    .thenReturn(Optional.of(newCategory));

            when(regionRepository.findById(regionId))
                    .thenReturn(Optional.of(newRegion));

            // when
            OwnerStoreDetailResponse response =
                    ownerStoreService.updateStore(
                            storeId,
                            request,
                            ownerDetails
                    );

            // then
            assertThat(response).isNotNull();

            verify(store).update(
                    newCategory,
                    newRegion,
                    request.storeName(),
                    request.storeTel(),
                    request.storeImage(),
                    request.address(),
                    request.minOrderPrice(),
                    request.deliveryFee(),
                    request.freeDeliveryAmount(),
                    request.openTime(),
                    request.closeTime()
            );
        }

        @Test
        @DisplayName("카테고리 ID가 없으면 기존 카테고리를 유지한다")
        void updateStore_withoutCategory() {
            // given
            Store store = mockStore(StoreOperationStatus.ACTIVE);

            StoreUpdateRequest request = new StoreUpdateRequest(
                    null,
                    regionId,
                    "수정된 가게",
                    "02-1234-5678",
                    "https://example.com/store.jpg",
                    "서울특별시 송파구",
                    20000,
                    3500,
                    40000,
                    LocalTime.of(10, 0),
                    LocalTime.of(23, 0)
            );

            Region newRegion = mock(Region.class);

            when(
                    storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(
                            storeId,
                            ownerId
                    )
            ).thenReturn(Optional.of(store));

            when(regionRepository.findById(regionId))
                    .thenReturn(Optional.of(newRegion));

            // when
            ownerStoreService.updateStore(
                    storeId,
                    request,
                    ownerDetails
            );

            // then
            verifyNoInteractions(categoryRepository);

            verify(store).update(
                    null,
                    newRegion,
                    request.storeName(),
                    request.storeTel(),
                    request.storeImage(),
                    request.address(),
                    request.minOrderPrice(),
                    request.deliveryFee(),
                    request.freeDeliveryAmount(),
                    request.openTime(),
                    request.closeTime()
            );
        }

        @Test
        @DisplayName("존재하지 않는 가게는 수정할 수 없다")
        void updateStore_storeNotFound() {
            // given
            StoreUpdateRequest request = createUpdateRequest();

            when(
                    storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(
                            storeId,
                            ownerId
                    )
            ).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    ownerStoreService.updateStore(
                            storeId,
                            request,
                            ownerDetails
                    )
            ).isInstanceOf(CustomException.class);

            verifyNoInteractions(
                    categoryRepository,
                    regionRepository
            );
        }

        @Test
        @DisplayName("CLOSE_REQUESTED 상태이면 수정할 수 없다")
        void updateStore_closeRequested() {
            // given
            Store store =
                    mockStore(StoreOperationStatus.CLOSE_REQUESTED);

            StoreUpdateRequest request = createUpdateRequest();

            when(
                    storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(
                            storeId,
                            ownerId
                    )
            ).thenReturn(Optional.of(store));

            // when & then
            assertThatThrownBy(() ->
                    ownerStoreService.updateStore(
                            storeId,
                            request,
                            ownerDetails
                    )
            ).isInstanceOf(CustomException.class);

            verifyNoInteractions(
                    categoryRepository,
                    regionRepository
            );

            verify(store, never()).update(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
            );
        }

        @Test
        @DisplayName("CLOSED 상태이면 수정할 수 없다")
        void updateStore_closed() {
            // given
            Store store = mockStore(StoreOperationStatus.CLOSED);
            StoreUpdateRequest request = createUpdateRequest();

            when(
                    storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(
                            storeId,
                            ownerId
                    )
            ).thenReturn(Optional.of(store));

            // when & then
            assertThatThrownBy(() ->
                    ownerStoreService.updateStore(
                            storeId,
                            request,
                            ownerDetails
                    )
            ).isInstanceOf(CustomException.class);

            verifyNoInteractions(
                    categoryRepository,
                    regionRepository
            );

            verify(store, never()).update(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
            );
        }

        @Test
        @DisplayName("존재하지 않는 카테고리이면 수정할 수 없다")
        void updateStore_categoryNotFound() {
            // given
            Store store = mockStore(StoreOperationStatus.ACTIVE);
            StoreUpdateRequest request = createUpdateRequest();

            when(
                    storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(
                            storeId,
                            ownerId
                    )
            ).thenReturn(Optional.of(store));

            when(categoryRepository.findById(categoryId))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    ownerStoreService.updateStore(
                            storeId,
                            request,
                            ownerDetails
                    )
            ).isInstanceOf(CustomException.class);

            verifyNoInteractions(regionRepository);

            verify(store, never()).update(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
            );
        }

        @Test
        @DisplayName("존재하지 않는 지역이면 수정할 수 없다")
        void updateStore_regionNotFound() {
            // given
            Store store = mockStore(StoreOperationStatus.ACTIVE);
            Category newCategory = mock(Category.class);
            StoreUpdateRequest request = createUpdateRequest();

            when(
                    storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(
                            storeId,
                            ownerId
                    )
            ).thenReturn(Optional.of(store));

            when(categoryRepository.findById(categoryId))
                    .thenReturn(Optional.of(newCategory));

            when(regionRepository.findById(regionId))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    ownerStoreService.updateStore(
                            storeId,
                            request,
                            ownerDetails
                    )
            ).isInstanceOf(CustomException.class);

            verify(store, never()).update(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
            );
        }
    }

    @Nested
    @DisplayName("가게 삭제 요청")
    class RequestDeleteStoreTest {

        @Test
        @DisplayName("가게 삭제 요청에 성공한다")
        void requestDeleteStore_success() {
            // given
            Store store = mockStore(StoreOperationStatus.ACTIVE);

            when(
                    storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(
                            storeId,
                            ownerId
                    )
            ).thenReturn(Optional.of(store));

            // when
            ownerStoreService.requestDeleteStore(
                    storeId,
                    ownerDetails
            );

            // then
            verify(store).requestDelete();
        }

        @Test
        @DisplayName("존재하지 않거나 다른 사용자의 가게는 삭제 요청할 수 없다")
        void requestDeleteStore_notFound() {
            // given
            when(
                    storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(
                            storeId,
                            ownerId
                    )
            ).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    ownerStoreService.requestDeleteStore(
                            storeId,
                            ownerDetails
                    )
            ).isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("이미 삭제 요청된 가게는 다시 요청할 수 없다")
        void requestDeleteStore_alreadyRequested() {
            // given
            Store store =
                    mockStore(StoreOperationStatus.CLOSE_REQUESTED);

            when(
                    storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(
                            storeId,
                            ownerId
                    )
            ).thenReturn(Optional.of(store));

            // when & then
            assertThatThrownBy(() ->
                    ownerStoreService.requestDeleteStore(
                            storeId,
                            ownerDetails
                    )
            ).isInstanceOf(CustomException.class);

            verify(store, never()).requestDelete();
        }


    }

    private Store mockStore(
            StoreOperationStatus operationStatus
    ) {
        Store store = mock(Store.class);

        User owner = mock(User.class);
        Category category = mock(Category.class);
        Region region = mock(Region.class);

        lenient().when(store.getId())
                .thenReturn(storeId);

        lenient().when(store.getOwner())
                .thenReturn(owner);

        lenient().when(store.getCategory())
                .thenReturn(category);

        lenient().when(store.getRegion())
                .thenReturn(region);

        lenient().when(store.getStoreName())
                .thenReturn("테스트가게");

        lenient().when(store.getStoreTel())
                .thenReturn("010-1234-5678");

        lenient().when(store.getStoreImage())
                .thenReturn(null);

        lenient().when(store.getAddress())
                .thenReturn("서울특별시 강남구 테헤란로 123");

        lenient().when(store.getMinOrderPrice())
                .thenReturn(15000);

        lenient().when(store.getDeliveryFee())
                .thenReturn(3000);

        lenient().when(store.getFreeDeliveryAmount())
                .thenReturn(30000);

        lenient().when(store.getStoreRating())
                .thenReturn(BigDecimal.ZERO);

        lenient().when(store.getReviewCount())
                .thenReturn(0);

        lenient().when(store.getOpenTime())
                .thenReturn(LocalTime.of(9, 0));

        lenient().when(store.getCloseTime())
                .thenReturn(LocalTime.of(22, 0));

        lenient().when(store.getOperationStatus())
                .thenReturn(operationStatus);

        lenient().when(store.isOpen())
                .thenReturn(
                        operationStatus == StoreOperationStatus.ACTIVE
                );

        lenient().when(owner.getId())
                .thenReturn(ownerId);

        lenient().when(owner.getEmail())
                .thenReturn("owner@test.com");

        lenient().when(owner.getNickname())
                .thenReturn("테스트 사장님");

        lenient().when(category.getId())
                .thenReturn(categoryId);

        lenient().when(category.getName())
                .thenReturn("치킨");

        lenient().when(region.getId())
                .thenReturn(regionId);

        lenient().when(region.getName())
                .thenReturn("서울 강남구");

        return store;
    }

    private StoreUpdateRequest createUpdateRequest() {
        return new StoreUpdateRequest(
                categoryId,
                regionId,
                "수정된 가게",
                "02-1234-5678",
                "https://example.com/store.jpg",
                "서울특별시 송파구",
                20000,
                3500,
                40000,
                LocalTime.of(10, 0),
                LocalTime.of(23, 0)
        );
    }
}