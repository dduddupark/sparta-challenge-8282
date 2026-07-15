package com.sparta.spartachallenge8282.store.application;

import com.sparta.spartachallenge8282.category.domain.Category;
import com.sparta.spartachallenge8282.global.common.PageResponse;
import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import com.sparta.spartachallenge8282.region.domain.Region;
import com.sparta.spartachallenge8282.store.application.validator.StoreAuthorizationValidator;
import com.sparta.spartachallenge8282.store.domain.Store;
import com.sparta.spartachallenge8282.store.domain.StoreApplication;
import com.sparta.spartachallenge8282.store.domain.StoreApplicationRepository;
import com.sparta.spartachallenge8282.store.domain.StoreApplicationStatus;
import com.sparta.spartachallenge8282.store.domain.StoreOperationStatus;
import com.sparta.spartachallenge8282.store.domain.StoreRepository;
import com.sparta.spartachallenge8282.store.presentation.dto.request.StoreRejectRequest;
import com.sparta.spartachallenge8282.store.presentation.dto.response.AdminStoreApplicationDetailResponse;
import com.sparta.spartachallenge8282.store.presentation.dto.response.AdminStoreApplicationListResponse;
import com.sparta.spartachallenge8282.store.presentation.dto.response.AdminStoreDetailResponse;
import com.sparta.spartachallenge8282.store.presentation.dto.response.AdminStoreListResponse;
import com.sparta.spartachallenge8282.store.presentation.dto.response.StoreApplicationProcessResponse;
import com.sparta.spartachallenge8282.user.domain.User;
import com.sparta.spartachallenge8282.user.domain.UserRole;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminStoreService 테스트")
class AdminStoreServiceTest {

    @Mock
    private StoreApplicationRepository storeApplicationRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreAuthorizationValidator validator;

    @InjectMocks
    private AdminStoreService adminStoreService;

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
        applicationId = UUID.randomUUID();
        storeId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        regionId = UUID.randomUUID();

        managerDetails = new UserDetailsImpl(
                2L,
                "manager@test.com",
                UserRole.MANAGER.getAuthority()
        );

        applicant = mock(User.class);
        category = mock(Category.class);
        region = mock(Region.class);

        lenient().when(applicant.getId())
                .thenReturn(1L);

        lenient().when(applicant.getEmail())
                .thenReturn("owner@test.com");

        lenient().when(applicant.getNickname())
                .thenReturn("테스트 사장님");

        lenient().when(applicant.getRole())
                .thenReturn(UserRole.CUSTOMER);

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
    @DisplayName("가게 등록 신청 목록 조회")
    class GetStoreApplicationsTest {

        @Test
        @DisplayName("상태가 없으면 전체 등록 신청을 조회한다")
        void getStoreApplications_all() {
            // given
            PageRequest pageable = PageRequest.of(0, 20);

            StoreApplication application =
                    mockApplication(StoreApplicationStatus.PENDING);

            Page<StoreApplication> applications =
                    new PageImpl<>(
                            List.of(application),
                            pageable,
                            1
                    );

            when(storeApplicationRepository.findAll(pageable))
                    .thenReturn(applications);

            // when
            PageResponse<AdminStoreApplicationListResponse> response =
                    adminStoreService.getAdminStoreApplications(
                            null,
                            pageable
                    );

            // then
            assertThat(response).isNotNull();

            verify(storeApplicationRepository)
                    .findAll(pageable);

            verify(storeApplicationRepository, never())
                    .findAllByStatus(any(), any());
        }

        @Test
        @DisplayName("상태를 전달하면 해당 상태의 등록 신청만 조회한다")
        void getStoreApplications_byStatus() {
            // given
            PageRequest pageable = PageRequest.of(0, 20);

            StoreApplicationStatus status =
                    StoreApplicationStatus.PENDING;

            StoreApplication application =
                    mockApplication(status);

            Page<StoreApplication> applications =
                    new PageImpl<>(
                            List.of(application),
                            pageable,
                            1
                    );

            when(
                    storeApplicationRepository.findAllByStatus(
                            status,
                            pageable
                    )
            ).thenReturn(applications);

            // when
            PageResponse<AdminStoreApplicationListResponse> response =
                    adminStoreService.getAdminStoreApplications(
                            status,
                            pageable
                    );

            // then
            assertThat(response).isNotNull();

            verify(storeApplicationRepository)
                    .findAllByStatus(status, pageable);

            verify(storeApplicationRepository, never())
                    .findAll(pageable);
        }
    }

    @Nested
    @DisplayName("가게 등록 신청 상세 조회")
    class GetStoreApplicationTest {

        @Test
        @DisplayName("등록 신청 상세 조회에 성공한다")
        void getStoreApplication_success() {
            // given
            StoreApplication application =
                    mockApplication(StoreApplicationStatus.PENDING);

            when(storeApplicationRepository.findById(applicationId))
                    .thenReturn(Optional.of(application));

            // when
            AdminStoreApplicationDetailResponse response =
                    adminStoreService.getAdminStoreApplication(
                            applicationId
                    );

            // then
            assertThat(response).isNotNull();

            verify(storeApplicationRepository)
                    .findById(applicationId);
        }

        @Test
        @DisplayName("존재하지 않는 등록 신청 조회 시 예외가 발생한다")
        void getStoreApplication_notFound() {
            // given
            when(storeApplicationRepository.findById(applicationId))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    adminStoreService.getAdminStoreApplication(
                            applicationId
                    )
            ).isInstanceOf(CustomException.class);

            verify(storeApplicationRepository)
                    .findById(applicationId);
        }
    }

    @Nested
    @DisplayName("관리자 가게 목록 조회")
    class GetStoresTest {

        @Test
        @DisplayName("상태가 없으면 삭제된 가게를 포함한 전체 가게를 조회한다")
        void getStores_all() {
            // given
            PageRequest pageable = PageRequest.of(0, 20);

            Store store =
                    mockStore(StoreOperationStatus.ACTIVE);

            Page<Store> stores =
                    new PageImpl<>(
                            List.of(store),
                            pageable,
                            1
                    );

            when(storeRepository.findAll(pageable))
                    .thenReturn(stores);

            // when
            PageResponse<AdminStoreListResponse> response =
                    adminStoreService.getStores(
                            null,
                            pageable
                    );

            // then
            assertThat(response).isNotNull();

            verify(storeRepository)
                    .findAll(pageable);

            verify(storeRepository, never())
                    .findAllByOperationStatus(any(), any());
        }

        @Test
        @DisplayName("운영 상태를 전달하면 해당 상태의 가게만 조회한다")
        void getStores_byStatus() {
            // given
            PageRequest pageable = PageRequest.of(0, 20);

            StoreOperationStatus status =
                    StoreOperationStatus.ACTIVE;

            Store store =
                    mockStore(status);

            Page<Store> stores =
                    new PageImpl<>(
                            List.of(store),
                            pageable,
                            1
                    );

            when(
                    storeRepository.findAllByOperationStatus(
                            status,
                            pageable
                    )
            ).thenReturn(stores);

            // when
            PageResponse<AdminStoreListResponse> response =
                    adminStoreService.getStores(
                            status,
                            pageable
                    );

            // then
            assertThat(response).isNotNull();

            verify(storeRepository)
                    .findAllByOperationStatus(
                            status,
                            pageable
                    );

            verify(storeRepository, never())
                    .findAll(pageable);
        }

        @Test
        @DisplayName("조회되는 가게가 없으면 빈 페이지 응답을 반환한다")
        void getStores_empty() {
            // given
            PageRequest pageable = PageRequest.of(0, 20);

            StoreOperationStatus status =
                    StoreOperationStatus.PREPARING;

            Page<Store> stores =
                    Page.empty(pageable);

            when(
                    storeRepository.findAllByOperationStatus(
                            status,
                            pageable
                    )
            ).thenReturn(stores);

            // when
            PageResponse<AdminStoreListResponse> response =
                    adminStoreService.getStores(
                            status,
                            pageable
                    );

            // then
            assertThat(response).isNotNull();

            verify(storeRepository)
                    .findAllByOperationStatus(
                            status,
                            pageable
                    );
        }
    }

    @Nested
    @DisplayName("관리자 가게 상세 조회")
    class GetStoreTest {

        @Test
        @DisplayName("삭제 여부와 관계없이 가게 상세 조회에 성공한다")
        void getStore_success() {
            // given
            Store store =
                    mockStore(StoreOperationStatus.ACTIVE);

            when(storeRepository.findById(storeId))
                    .thenReturn(Optional.of(store));

            // when
            AdminStoreDetailResponse response =
                    adminStoreService.getStore(storeId);

            // then
            assertThat(response).isNotNull();

            verify(storeRepository)
                    .findById(storeId);
        }

        @Test
        @DisplayName("존재하지 않는 가게 조회 시 예외가 발생한다")
        void getStore_notFound() {
            // given
            when(storeRepository.findById(storeId))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    adminStoreService.getStore(storeId)
            ).isInstanceOf(CustomException.class);

            verify(storeRepository)
                    .findById(storeId);
        }
    }

    @Nested
    @DisplayName("가게 등록 승인")
    class ApproveStoreTest {

        @Test
        @DisplayName("PENDING 신청을 승인하면 가게가 생성되고 신청자는 OWNER로 승격된다")
        void approveStore_success() {
            // given
            StoreApplication application =
                    mockApplication(StoreApplicationStatus.PENDING);

            Store savedStore =
                    mockStore(StoreOperationStatus.PREPARING);

            when(storeApplicationRepository.findById(applicationId))
                    .thenReturn(Optional.of(application));

            when(storeRepository.save(any(Store.class)))
                    .thenReturn(savedStore);

            // when
            StoreApplicationProcessResponse response =
                    adminStoreService.approveStore(
                            applicationId,
                            managerDetails
                    );

            // then
            assertThat(response).isNotNull();

            verify(validator)
                    .validateManagerRole(managerDetails);

            verify(application)
                    .approve();

            verify(storeRepository)
                    .save(any(Store.class));

            verify(applicant)
                    .promoteToOwner();
        }

        @Test
        @DisplayName("이미 처리된 신청은 승인할 수 없다")
        void approveStore_invalidStatus() {
            // given
            StoreApplication application =
                    mockApplication(StoreApplicationStatus.APPROVED);

            when(storeApplicationRepository.findById(applicationId))
                    .thenReturn(Optional.of(application));

            // when & then
            assertThatThrownBy(() ->
                    adminStoreService.approveStore(
                            applicationId,
                            managerDetails
                    )
            ).isInstanceOf(CustomException.class);

            verify(validator)
                    .validateManagerRole(managerDetails);

            verify(application, never())
                    .approve();

            verify(storeRepository, never())
                    .save(any(Store.class));

            verify(applicant, never())
                    .promoteToOwner();
        }

        @Test
        @DisplayName("존재하지 않는 신청은 승인할 수 없다")
        void approveStore_applicationNotFound() {
            // given
            when(storeApplicationRepository.findById(applicationId))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    adminStoreService.approveStore(
                            applicationId,
                            managerDetails
                    )
            ).isInstanceOf(CustomException.class);

            verify(validator)
                    .validateManagerRole(managerDetails);

            verify(storeRepository, never())
                    .save(any(Store.class));
        }
    }

    @Nested
    @DisplayName("가게 등록 거절")
    class RejectStoreTest {

        @Test
        @DisplayName("PENDING 신청은 거절할 수 있다")
        void rejectStore_success() {
            // given
            StoreApplication application =
                    mockApplication(StoreApplicationStatus.PENDING);

            StoreRejectRequest request =
                    new StoreRejectRequest(
                            "사업자 정보가 일치하지 않습니다."
                    );

            when(storeApplicationRepository.findById(applicationId))
                    .thenReturn(Optional.of(application));

            // when
            StoreApplicationProcessResponse response =
                    adminStoreService.rejectStore(
                            applicationId,
                            request,
                            managerDetails
                    );

            // then
            assertThat(response).isNotNull();

            verify(validator)
                    .validateManagerRole(managerDetails);

            verify(application)
                    .reject("사업자 정보가 일치하지 않습니다.");

            verify(storeRepository, never())
                    .save(any(Store.class));

            verify(applicant, never())
                    .promoteToOwner();
        }

        @Test
        @DisplayName("거절 사유가 공백이면 거절할 수 없다")
        void rejectStore_blankReason() {
            // given
            StoreApplication application =
                    mockApplication(StoreApplicationStatus.PENDING);

            StoreRejectRequest request =
                    new StoreRejectRequest(" ");

            when(storeApplicationRepository.findById(applicationId))
                    .thenReturn(Optional.of(application));

            // when & then
            assertThatThrownBy(() ->
                    adminStoreService.rejectStore(
                            applicationId,
                            request,
                            managerDetails
                    )
            ).isInstanceOf(CustomException.class);

            verify(validator)
                    .validateManagerRole(managerDetails);

            verify(application, never())
                    .reject(any());

            verify(storeRepository, never())
                    .save(any(Store.class));
        }

        @Test
        @DisplayName("거절 사유가 null이면 거절할 수 없다")
        void rejectStore_nullReason() {
            // given
            StoreApplication application =
                    mockApplication(StoreApplicationStatus.PENDING);

            StoreRejectRequest request =
                    new StoreRejectRequest(null);

            when(storeApplicationRepository.findById(applicationId))
                    .thenReturn(Optional.of(application));

            // when & then
            assertThatThrownBy(() ->
                    adminStoreService.rejectStore(
                            applicationId,
                            request,
                            managerDetails
                    )
            ).isInstanceOf(CustomException.class);

            verify(validator)
                    .validateManagerRole(managerDetails);

            verify(application, never())
                    .reject(any());
        }

        @Test
        @DisplayName("이미 처리된 신청은 거절할 수 없다")
        void rejectStore_invalidStatus() {
            // given
            StoreApplication application =
                    mockApplication(StoreApplicationStatus.APPROVED);

            StoreRejectRequest request =
                    new StoreRejectRequest("거절 사유");

            when(storeApplicationRepository.findById(applicationId))
                    .thenReturn(Optional.of(application));

            // when & then
            assertThatThrownBy(() ->
                    adminStoreService.rejectStore(
                            applicationId,
                            request,
                            managerDetails
                    )
            ).isInstanceOf(CustomException.class);

            verify(validator)
                    .validateManagerRole(managerDetails);

            verify(application, never())
                    .reject(any());
        }

        @Test
        @DisplayName("존재하지 않는 신청은 거절할 수 없다")
        void rejectStore_applicationNotFound() {
            // given
            StoreRejectRequest request =
                    new StoreRejectRequest("거절 사유");

            when(storeApplicationRepository.findById(applicationId))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    adminStoreService.rejectStore(
                            applicationId,
                            request,
                            managerDetails
                    )
            ).isInstanceOf(CustomException.class);

            verify(validator)
                    .validateManagerRole(managerDetails);
        }
    }

    @Nested
    @DisplayName("가게 삭제 승인")
    class ApproveDeleteStoreTest {

        @Test
        @DisplayName("CLOSE_REQUESTED 상태인 가게는 삭제 승인할 수 있다")
        void approveDeleteStore_success() {
            // given
            Store store =
                    mockStore(StoreOperationStatus.CLOSE_REQUESTED);

            when(storeRepository.findByIdAndDeletedAtIsNull(storeId))
                    .thenReturn(Optional.of(store));

            // when
            adminStoreService.approveDeleteStore(
                    storeId,
                    managerDetails
            );

            // then
            verify(validator)
                    .validateManagerRole(managerDetails);

            verify(storeRepository)
                    .findByIdAndDeletedAtIsNull(storeId);

            verify(store)
                    .approveDelete(managerDetails.userId());
        }

        @Test
        @DisplayName("삭제 요청 상태가 아니면 삭제 승인할 수 없다")
        void approveDeleteStore_notRequested() {
            // given
            Store store =
                    mockStore(StoreOperationStatus.ACTIVE);

            when(storeRepository.findByIdAndDeletedAtIsNull(storeId))
                    .thenReturn(Optional.of(store));

            // when & then
            assertThatThrownBy(() ->
                    adminStoreService.approveDeleteStore(
                            storeId,
                            managerDetails
                    )
            ).isInstanceOf(CustomException.class);

            verify(validator)
                    .validateManagerRole(managerDetails);

            verify(storeRepository)
                    .findByIdAndDeletedAtIsNull(storeId);

            verify(store, never())
                    .approveDelete(anyLong());
        }

        @Test
        @DisplayName("존재하지 않거나 이미 삭제된 가게는 삭제 승인할 수 없다")
        void approveDeleteStore_notFound() {
            // given
            when(storeRepository.findByIdAndDeletedAtIsNull(storeId))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    adminStoreService.approveDeleteStore(
                            storeId,
                            managerDetails
                    )
            ).isInstanceOf(CustomException.class);

            verify(validator)
                    .validateManagerRole(managerDetails);

            verify(storeRepository)
                    .findByIdAndDeletedAtIsNull(storeId);
        }

        @Test
        @DisplayName("관리자 권한 검증에 실패하면 가게를 조회하지 않는다")
        void approveDeleteStore_accessDenied() {
            // given
            doThrow(CustomException.class)
                    .when(validator)
                    .validateManagerRole(managerDetails);

            // when & then
            assertThatThrownBy(() ->
                    adminStoreService.approveDeleteStore(
                            storeId,
                            managerDetails
                    )
            ).isInstanceOf(CustomException.class);

            verify(validator)
                    .validateManagerRole(managerDetails);

            verifyNoInteractions(storeRepository);
        }
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

    private Store mockStore(
            StoreOperationStatus operationStatus
    ) {
        Store store =
                mock(Store.class);

        lenient().when(store.getId())
                .thenReturn(storeId);

        lenient().when(store.getOwner())
                .thenReturn(applicant);

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

        return store;
    }
}