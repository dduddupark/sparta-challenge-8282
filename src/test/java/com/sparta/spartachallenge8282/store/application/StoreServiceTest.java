package com.sparta.spartachallenge8282.store.application;

import com.sparta.spartachallenge8282.category.domain.Category;
import com.sparta.spartachallenge8282.global.common.PageResponse;
import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.region.domain.Region;
import com.sparta.spartachallenge8282.store.domain.Store;
import com.sparta.spartachallenge8282.store.domain.StoreApplication;
import com.sparta.spartachallenge8282.store.domain.StoreApplicationStatus;
import com.sparta.spartachallenge8282.store.domain.StoreOperationStatus;
import com.sparta.spartachallenge8282.store.domain.StoreRepository;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StoreService 테스트")
class StoreServiceTest {

    @Mock
    private StoreRepository storeRepository;

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
    @DisplayName("일반 사용자 및 비회원 가게 조회")
    class PublicStoreTest {

        @Test
        @DisplayName("ACTIVE 상태인 가게 목록을 페이징 조회한다")
        void getStores_success() {
            // given
            PageRequest pageable = PageRequest.of(0, 20);
            Store store = createRealStore();

            Page<Store> stores = new PageImpl<>(
                    List.of(store),
                    pageable,
                    1
            );

            when(
                    storeRepository
                            .findAllByOperationStatusAndDeletedAtIsNull(
                                    StoreOperationStatus.ACTIVE,
                                    pageable
                            )
            ).thenReturn(stores);

            // when
            PageResponse<UserStoreListResponse> response =
                    storeService.getStores(pageable);

            // then
            assertThat(response).isNotNull();

            verify(storeRepository)
                    .findAllByOperationStatusAndDeletedAtIsNull(
                            StoreOperationStatus.ACTIVE,
                            pageable
                    );
        }

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

        ReflectionTestUtils.setField(store, "id", storeId);
        ReflectionTestUtils.setField(
                store,
                "operationStatus",
                StoreOperationStatus.ACTIVE
        );

        return store;
    }
}