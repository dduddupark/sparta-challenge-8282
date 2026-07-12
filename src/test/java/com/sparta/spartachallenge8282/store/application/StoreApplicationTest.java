package com.sparta.spartachallenge8282.store.application;

import com.sparta.spartachallenge8282.category.domain.Category;
import com.sparta.spartachallenge8282.category.domain.CategoryRepository;
import com.sparta.spartachallenge8282.global.common.PageResponse;
import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import com.sparta.spartachallenge8282.region.domain.Region;
import com.sparta.spartachallenge8282.region.domain.RegionRepository;
import com.sparta.spartachallenge8282.store.domain.*;
import com.sparta.spartachallenge8282.store.presentation.dto.request.StoreApplicationRequest;
import com.sparta.spartachallenge8282.store.presentation.dto.request.StoreRejectRequest;
import com.sparta.spartachallenge8282.store.presentation.dto.response.*;
import com.sparta.spartachallenge8282.user.entity.User;
import com.sparta.spartachallenge8282.user.entity.UserRole;
import com.sparta.spartachallenge8282.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StoreApplicationRepository storeApplicationRepository;

    @InjectMocks
    private StoreService storeService;

    private UserDetailsImpl customerDetails;
    private UserDetailsImpl managerDetails;

    private User applicant;
    private Category category;
    private Region region;

    private UUID applicationId;
    private UUID storeId;
    private UUID categoryId;
    private UUID regionId;

    @BeforeEach
    void setUp() {
        customerDetails = new UserDetailsImpl(
                1L,
                "customer@test.com",
                UserRole.CUSTOMER.getAuthority()
        );

        managerDetails = new UserDetailsImpl(
                2L,
                "manager@test.com",
                UserRole.MANAGER.getAuthority()
        );

        applicant = mock(User.class);
        category = mock(Category.class);
        region = mock(Region.class);

        applicationId = UUID.randomUUID();
        storeId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        regionId = UUID.randomUUID();

        lenient().when(applicant.getId()).thenReturn(1L);
        lenient().when(applicant.getEmail())
                .thenReturn("customer@test.com");
        lenient().when(applicant.getNickname())
                .thenReturn("테스트사용자");
        lenient().when(applicant.getRole())
                .thenReturn(UserRole.CUSTOMER);

        lenient().when(category.getId()).thenReturn(categoryId);
        lenient().when(region.getId()).thenReturn(regionId);


    }

    @Nested
    @DisplayName("가게 등록 신청")
    class CreateStoreApplicationTest {

        @Test
        @DisplayName("CUSTOMER가 가게 등록을 신청하면 PENDING 신청이 저장된다")
        void createStoreApplication_success() {
            StoreApplicationRequest request = createRequest();

            when(categoryRepository.findById(categoryId))
                    .thenReturn(Optional.of(category));

            when(regionRepository.findById(regionId))
                    .thenReturn(Optional.of(region));

            when(userRepository.findById(customerDetails.userId()))
                    .thenReturn(Optional.of(applicant));

            when(storeApplicationRepository.save(any(StoreApplication.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            MyStoreApplicationCreateResponse response =
                    storeService.createStoreApplication(
                            request,
                            customerDetails
                    );

            ArgumentCaptor<StoreApplication> captor =
                    ArgumentCaptor.forClass(StoreApplication.class);

            verify(storeApplicationRepository).save(captor.capture());

            StoreApplication savedApplication = captor.getValue();

            assertThat(response).isNotNull();
            assertThat(savedApplication.getApplicant())
                    .isEqualTo(applicant);
            assertThat(savedApplication.getCategory())
                    .isEqualTo(category);
            assertThat(savedApplication.getRegion())
                    .isEqualTo(region);
            assertThat(savedApplication.getStoreName())
                    .isEqualTo("테스트가게");
            assertThat(savedApplication.getStoreTel())
                    .isEqualTo("010-1234-5678");
            assertThat(savedApplication.getStatus())
                    .isEqualTo(StoreApplicationStatus.PENDING);
        }

        @Test
        @DisplayName("MANAGER는 가게 등록을 신청할 수 없다")
        void createStoreApplication_invalidRole() {
            StoreApplicationRequest request = createRequest();

            assertThatThrownBy(() ->
                    storeService.createStoreApplication(
                            request,
                            managerDetails
                    )
            ).isInstanceOf(CustomException.class);

            verifyNoInteractions(
                    categoryRepository,
                    regionRepository,
                    userRepository,
                    storeApplicationRepository
            );
        }
    }

    @Nested
    @DisplayName("가게 등록 승인")
    class ApproveStoreTest {

        @Test
        @DisplayName("PENDING 신청을 승인하면 Store가 생성되고 신청자는 OWNER로 승격된다")
        void approveStore_success() {
            StoreApplication application =
                    mockApplication(StoreApplicationStatus.PENDING);

            Store savedStore = mockSavedStore();

            when(storeApplicationRepository.findById(applicationId))
                    .thenReturn(Optional.of(application));

            when(storeRepository.save(any(Store.class)))
                    .thenReturn(savedStore);

            StoreApplicationProcessResponse response =
                    storeService.approveStore(
                            applicationId,
                            managerDetails
                    );

            assertThat(response).isNotNull();

            verify(application).approve();
            verify(storeRepository).save(any(Store.class));
            verify(applicant).promoteToOwner();
        }

        @Test
        @DisplayName("이미 처리된 신청은 승인할 수 없다")
        void approveStore_invalidStatus() {
            StoreApplication application =
                    mockApplication(StoreApplicationStatus.APPROVED);

            when(storeApplicationRepository.findById(applicationId))
                    .thenReturn(Optional.of(application));

            assertThatThrownBy(() ->
                    storeService.approveStore(
                            applicationId,
                            managerDetails
                    )
            ).isInstanceOf(CustomException.class);

            verify(application, never()).approve();
            verify(storeRepository, never()).save(any(Store.class));
            verify(applicant, never()).promoteToOwner();
        }

        @Test
        @DisplayName("존재하지 않는 신청은 승인할 수 없다")
        void approveStore_applicationNotFound() {
            when(storeApplicationRepository.findById(applicationId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    storeService.approveStore(
                            applicationId,
                            managerDetails
                    )
            ).isInstanceOf(CustomException.class);

            verify(storeRepository, never()).save(any(Store.class));
            verify(applicant, never()).promoteToOwner();
        }
    }

    @Nested
    @DisplayName("가게 등록 거절")
    class RejectStoreTest {

        @Test
        @DisplayName("PENDING 신청은 거절할 수 있고 Store는 생성되지 않는다")
        void rejectStore_success() {
            StoreApplication application =
                    mockApplication(StoreApplicationStatus.PENDING);

            StoreRejectRequest request =
                    new StoreRejectRequest(
                            "사업자 정보가 일치하지 않습니다."
                    );

            when(storeApplicationRepository.findById(applicationId))
                    .thenReturn(Optional.of(application));

            StoreApplicationProcessResponse response =
                    storeService.rejectStore(
                            applicationId,
                            request,
                            managerDetails
                    );

            assertThat(response).isNotNull();

            verify(application).reject(
                    "사업자 정보가 일치하지 않습니다."
            );

            verify(storeRepository, never())
                    .save(any(Store.class));

            verify(applicant, never())
                    .promoteToOwner();
        }

        @Test
        @DisplayName("거절 사유가 공백이면 거절할 수 없다")
        void rejectStore_blankReason() {
            StoreApplication application =
                    mockApplication(StoreApplicationStatus.PENDING);

            StoreRejectRequest request =
                    new StoreRejectRequest(" ");

            when(storeApplicationRepository.findById(applicationId))
                    .thenReturn(Optional.of(application));

            assertThatThrownBy(() ->
                    storeService.rejectStore(
                            applicationId,
                            request,
                            managerDetails
                    )
            ).isInstanceOf(CustomException.class);

            verify(application, never()).reject(anyString());
            verify(storeRepository, never())
                    .save(any(Store.class));
        }

        @Test
        @DisplayName("이미 처리된 신청은 거절할 수 없다")
        void rejectStore_invalidStatus() {
            StoreApplication application =
                    mockApplication(StoreApplicationStatus.APPROVED);

            StoreRejectRequest request =
                    new StoreRejectRequest("거절 사유");

            when(storeApplicationRepository.findById(applicationId))
                    .thenReturn(Optional.of(application));

            assertThatThrownBy(() ->
                    storeService.rejectStore(
                            applicationId,
                            request,
                            managerDetails
                    )
            ).isInstanceOf(CustomException.class);

            verify(application, never()).reject(anyString());
        }
    }

    @Nested
    @DisplayName("일반 사용자 및 비회원 가게 조회")
    class PublicStoreTest {

        @Test
        @DisplayName("가게 목록을 페이징 조회한다")
        void getStores_success() {
            PageRequest pageable = PageRequest.of(0, 20);

            Store store = createRealStore();

            Page<Store> stores =
                    new PageImpl<>(
                            List.of(store),
                            pageable,
                            1
                    );

            when(
                    storeRepository.findAllByOperationStatusAndDeletedAtIsNull(
                            StoreOperationStatus.ACTIVE,
                            pageable
                    )
            ).thenReturn(stores);

            PageResponse<UserStoreListResponse> response =
                    storeService.getStores(pageable);

            assertThat(response).isNotNull();

            verify(storeRepository)
                    .findAllByOperationStatusAndDeletedAtIsNull(
                            StoreOperationStatus.ACTIVE,
                            pageable
                    );
        }

        @Test
        @DisplayName("가게 상세 정보를 조회한다")
        void getStore_success() {
            Store store = createRealStore();

            when(
                    storeRepository.findByIdAndOperationStatusAndDeletedAtIsNull(
                            storeId,
                            StoreOperationStatus.ACTIVE
                    )
            ).thenReturn(Optional.of(store));

            UserStoreDetailResponse response =
                    storeService.getStore(storeId);

            assertThat(response).isNotNull();

            verify(storeRepository)
                    .findByIdAndOperationStatusAndDeletedAtIsNull(
                            storeId,
                            StoreOperationStatus.ACTIVE
                    );
        }

        @Test
        @DisplayName("존재하지 않는 가게 상세 조회 시 예외가 발생한다")
        void getStore_notFound() {

            when(
                    storeRepository.findByIdAndOperationStatusAndDeletedAtIsNull(
                            storeId,
                            StoreOperationStatus.ACTIVE
                    )
            ).thenReturn(Optional.empty());

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

    private StoreApplicationRequest createRequest() {
        return new StoreApplicationRequest(
                categoryId,
                regionId,
                "테스트가게",
                "010-1234-5678",
                null,
                "서울특별시 강남구 테헤란로 123",
                15000,
                3000,
                30000,
                LocalTime.of(9, 0),
                LocalTime.of(22, 0)
        );
    }

    private StoreApplication mockApplication(
            StoreApplicationStatus status
    ) {
        StoreApplication application =
                mock(StoreApplication.class);

        lenient().when(application.getId())
                .thenReturn(applicationId);

        lenient().when(application.getStatus())
                .thenReturn(status);

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

        return application;
    }

    private Store mockSavedStore() {
        Store store = mock(Store.class);

        lenient().when(store.getId()).thenReturn(storeId);
        lenient().when(store.getStoreName())
                .thenReturn("테스트가게");
        lenient().when(store.getOwner())
                .thenReturn(applicant);
        lenient().when(store.getCategory())
                .thenReturn(category);
        lenient().when(store.getRegion())
                .thenReturn(region);
        lenient().when(store.getStoreTel())
                .thenReturn("010-1234-5678");
        lenient().when(store.getAddress())
                .thenReturn("서울특별시 강남구 테헤란로 123");

        return store;
    }

    private Store createRealStore() {
        StoreApplication application =
                mockApplication(StoreApplicationStatus.APPROVED);

        return Store.from(application);
    }
}