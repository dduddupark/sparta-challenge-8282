package com.sparta.spartachallenge8282.store.application;

import com.sparta.spartachallenge8282.category.domain.Category;
import com.sparta.spartachallenge8282.category.domain.CategoryRepository;
import com.sparta.spartachallenge8282.global.common.PageResponse;
import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import com.sparta.spartachallenge8282.region.domain.Region;
import com.sparta.spartachallenge8282.region.domain.RegionRepository;
import com.sparta.spartachallenge8282.store.application.validator.StoreAuthorizationValidator;
import com.sparta.spartachallenge8282.store.domain.StoreApplication;
import com.sparta.spartachallenge8282.store.domain.StoreApplicationRepository;
import com.sparta.spartachallenge8282.store.domain.StoreApplicationStatus;
import com.sparta.spartachallenge8282.store.presentation.dto.request.StoreApplicationRequest;
import com.sparta.spartachallenge8282.store.presentation.dto.response.MyStoreApplicationCreateResponse;
import com.sparta.spartachallenge8282.store.presentation.dto.response.MyStoreApplicationDetailResponse;
import com.sparta.spartachallenge8282.store.presentation.dto.response.MyStoreApplicationListResponse;
import com.sparta.spartachallenge8282.user.domain.User;
import com.sparta.spartachallenge8282.user.domain.UserRole;
import com.sparta.spartachallenge8282.user.domain.UserRepository;
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

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StoreApplicationService 테스트")
class StoreApplicationServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StoreApplicationRepository storeApplicationRepository;

    @Mock
    private StoreAuthorizationValidator validator;

    @InjectMocks
    private StoreApplicationService storeApplicationService;

    private UserDetailsImpl customerDetails;
    private UserDetailsImpl managerDetails;

    private User applicant;
    private Category category;
    private Region region;

    private UUID applicationId;
    private UUID categoryId;
    private UUID regionId;

    @BeforeEach
    void setUp() {
        applicationId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        regionId = UUID.randomUUID();

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

        lenient().when(applicant.getId())
                .thenReturn(1L);

        lenient().when(applicant.getEmail())
                .thenReturn("customer@test.com");

        lenient().when(applicant.getNickname())
                .thenReturn("테스트사용자");

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
    @DisplayName("가게 등록 신청")
    class CreateStoreApplicationTest {

        @Test
        @DisplayName("CUSTOMER가 가게 등록을 신청하면 PENDING 신청이 저장된다")
        void createStoreApplication_success() {
            // given
            StoreApplicationRequest request = createRequest();

            when(categoryRepository.findById(categoryId))
                    .thenReturn(Optional.of(category));

            when(regionRepository.findById(regionId))
                    .thenReturn(Optional.of(region));

            when(userRepository.findById(customerDetails.userId()))
                    .thenReturn(Optional.of(applicant));

            when(storeApplicationRepository.save(any(StoreApplication.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // when
            MyStoreApplicationCreateResponse response =
                    storeApplicationService.createStoreApplication(
                            request,
                            customerDetails
                    );

            // then
            ArgumentCaptor<StoreApplication> captor =
                    ArgumentCaptor.forClass(StoreApplication.class);

            verify(storeApplicationRepository)
                    .save(captor.capture());

            verify(validator)
                    .validateStoreApplicationRole(customerDetails);

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
            // given
            StoreApplicationRequest request = createRequest();

            doThrow(new CustomException(ErrorCode.ACCESS_DENIED))
                    .when(validator)
                    .validateStoreApplicationRole(managerDetails);

            // when & then
            assertThatThrownBy(() ->
                    storeApplicationService.createStoreApplication(
                            request,
                            managerDetails
                    )
            ).isInstanceOf(CustomException.class);

            verify(validator)
                    .validateStoreApplicationRole(managerDetails);

            verifyNoInteractions(
                    categoryRepository,
                    regionRepository,
                    userRepository,
                    storeApplicationRepository
            );
        }

        @Test
        @DisplayName("존재하지 않는 카테고리이면 가게 등록 신청에 실패한다")
        void createStoreApplication_categoryNotFound() {
            // given
            StoreApplicationRequest request = createRequest();

            when(categoryRepository.findById(categoryId))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    storeApplicationService.createStoreApplication(
                            request,
                            customerDetails
                    )
            ).isInstanceOf(CustomException.class);

            verify(validator)
                    .validateStoreApplicationRole(customerDetails);

            verify(categoryRepository)
                    .findById(categoryId);

            verifyNoInteractions(
                    regionRepository,
                    userRepository,
                    storeApplicationRepository
            );
        }

        @Test
        @DisplayName("존재하지 않는 지역이면 가게 등록 신청에 실패한다")
        void createStoreApplication_regionNotFound() {
            // given
            StoreApplicationRequest request = createRequest();

            when(categoryRepository.findById(categoryId))
                    .thenReturn(Optional.of(category));

            when(regionRepository.findById(regionId))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    storeApplicationService.createStoreApplication(
                            request,
                            customerDetails
                    )
            ).isInstanceOf(CustomException.class);

            verify(regionRepository)
                    .findById(regionId);

            verifyNoInteractions(
                    userRepository,
                    storeApplicationRepository
            );
        }

        @Test
        @DisplayName("존재하지 않는 사용자이면 가게 등록 신청에 실패한다")
        void createStoreApplication_userNotFound() {
            // given
            StoreApplicationRequest request = createRequest();

            when(categoryRepository.findById(categoryId))
                    .thenReturn(Optional.of(category));

            when(regionRepository.findById(regionId))
                    .thenReturn(Optional.of(region));

            when(userRepository.findById(customerDetails.userId()))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    storeApplicationService.createStoreApplication(
                            request,
                            customerDetails
                    )
            ).isInstanceOf(CustomException.class);

            verify(userRepository)
                    .findById(customerDetails.userId());

            verify(storeApplicationRepository, never())
                    .save(any(StoreApplication.class));
        }
    }

    @Nested
    @DisplayName("내 가게 등록 신청 목록 조회")
    class GetMyStoreApplicationsTest {

        @Test
        @DisplayName("본인의 가게 등록 신청 목록을 페이징 조회한다")
        void getMyStoreApplications_success() {
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

            when(
                    storeApplicationRepository.findAllByApplicant_Id(
                            customerDetails.userId(),
                            pageable
                    )
            ).thenReturn(applications);

            // when
            PageResponse<MyStoreApplicationListResponse> response =
                    storeApplicationService.getMyStoreApplications(
                            customerDetails,
                            pageable
                    );

            // then
            assertThat(response).isNotNull();

            verify(storeApplicationRepository)
                    .findAllByApplicant_Id(
                            customerDetails.userId(),
                            pageable
                    );
        }

        @Test
        @DisplayName("등록 신청이 없으면 빈 페이지 응답을 반환한다")
        void getMyStoreApplications_empty() {
            // given
            PageRequest pageable = PageRequest.of(0, 20);
            Page<StoreApplication> applications =
                    Page.empty(pageable);

            when(
                    storeApplicationRepository.findAllByApplicant_Id(
                            customerDetails.userId(),
                            pageable
                    )
            ).thenReturn(applications);

            // when
            PageResponse<MyStoreApplicationListResponse> response =
                    storeApplicationService.getMyStoreApplications(
                            customerDetails,
                            pageable
                    );

            // then
            assertThat(response).isNotNull();

            verify(storeApplicationRepository)
                    .findAllByApplicant_Id(
                            customerDetails.userId(),
                            pageable
                    );
        }
    }

    @Nested
    @DisplayName("내 가게 등록 신청 상세 조회")
    class GetMyStoreApplicationTest {

        @Test
        @DisplayName("본인의 가게 등록 신청 상세 조회에 성공한다")
        void getMyStoreApplication_success() {
            // given
            StoreApplication application =
                    mockApplication(StoreApplicationStatus.PENDING);

            when(
                    storeApplicationRepository.findByIdAndApplicant_Id(
                            applicationId,
                            customerDetails.userId()
                    )
            ).thenReturn(Optional.of(application));

            // when
            MyStoreApplicationDetailResponse response =
                    storeApplicationService.getMyStoreApplication(
                            applicationId,
                            customerDetails
                    );

            // then
            assertThat(response).isNotNull();

            verify(storeApplicationRepository)
                    .findByIdAndApplicant_Id(
                            applicationId,
                            customerDetails.userId()
                    );
        }

        @Test
        @DisplayName("존재하지 않거나 본인의 신청이 아니면 예외가 발생한다")
        void getMyStoreApplication_notFound() {
            // given
            when(
                    storeApplicationRepository.findByIdAndApplicant_Id(
                            applicationId,
                            customerDetails.userId()
                    )
            ).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    storeApplicationService.getMyStoreApplication(
                            applicationId,
                            customerDetails
                    )
            ).isInstanceOf(CustomException.class);

            verify(storeApplicationRepository)
                    .findByIdAndApplicant_Id(
                            applicationId,
                            customerDetails.userId()
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

        lenient().when(application.getStatus())
                .thenReturn(status);

        lenient().when(application.getCreatedAt())
                .thenReturn(LocalDateTime.now());

        lenient().when(application.getApprovedAt())
                .thenReturn(
                        status == StoreApplicationStatus.APPROVED
                                ? LocalDateTime.now()
                                : null
                );

        lenient().when(application.getRejectedAt())
                .thenReturn(
                        status == StoreApplicationStatus.REJECTED
                                ? LocalDateTime.now()
                                : null
                );

        lenient().when(application.getRejectionReason())
                .thenReturn(
                        status == StoreApplicationStatus.REJECTED
                                ? "등록 조건 미충족"
                                : null
                );

        return application;
    }
}