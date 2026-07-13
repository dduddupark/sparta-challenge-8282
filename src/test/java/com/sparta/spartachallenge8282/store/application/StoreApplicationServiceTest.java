package com.sparta.spartachallenge8282.store.application;

import com.sparta.spartachallenge8282.category.domain.Category;
import com.sparta.spartachallenge8282.category.domain.CategoryRepository;
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

import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StoreApplicationService 테스트")
public class StoreApplicationServiceTest {
    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StoreApplicationRepository storeApplicationRepository;

    @Mock
    private StoreAuthorizationValidator authorizationValidator;

    @InjectMocks
    private StoreApplicationService storeApplicationService;

    private UserDetailsImpl customerDetails;
    private UserDetailsImpl managerDetails;
    private User applicant;
    private Category category;
    private Region region;
    private UUID categoryId;
    private UUID regionId;

    @BeforeEach
    void setUp() {
        customerDetails = new UserDetailsImpl(1L, "customer@test.com", UserRole.CUSTOMER.getAuthority());
        managerDetails = new UserDetailsImpl(2L, "manager@test.com", UserRole.MANAGER.getAuthority());

        applicant = mock(User.class);
        category = mock(Category.class);
        region = mock(Region.class);
        categoryId = UUID.randomUUID();
        regionId = UUID.randomUUID();

        lenient().when(applicant.getId()).thenReturn(1L);
        lenient().when(applicant.getEmail()).thenReturn("customer@test.com");
        lenient().when(applicant.getNickname()).thenReturn("테스트사용자");
        lenient().when(applicant.getRole()).thenReturn(UserRole.CUSTOMER);
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

            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
            when(regionRepository.findById(regionId)).thenReturn(Optional.of(region));
            when(userRepository.findById(customerDetails.userId())).thenReturn(Optional.of(applicant));
            when(storeApplicationRepository.save(any(StoreApplication.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            MyStoreApplicationCreateResponse response =
                    storeApplicationService.createStoreApplication(request, customerDetails);

            ArgumentCaptor<StoreApplication> captor = ArgumentCaptor.forClass(StoreApplication.class);
            verify(storeApplicationRepository).save(captor.capture());
            verify(authorizationValidator).validateStoreApplicationRole(customerDetails);

            StoreApplication savedApplication = captor.getValue();
            assertThat(response).isNotNull();
            assertThat(savedApplication.getApplicant()).isEqualTo(applicant);
            assertThat(savedApplication.getCategory()).isEqualTo(category);
            assertThat(savedApplication.getRegion()).isEqualTo(region);
            assertThat(savedApplication.getStoreName()).isEqualTo("테스트가게");
            assertThat(savedApplication.getStoreTel()).isEqualTo("010-1234-5678");
            assertThat(savedApplication.getStatus()).isEqualTo(StoreApplicationStatus.PENDING);
        }

        @Test
        @DisplayName("MANAGER는 가게 등록을 신청할 수 없다")
        void createStoreApplication_invalidRole() {
            StoreApplicationRequest request = createRequest();

            doThrow(new CustomException(ErrorCode.ACCESS_DENIED))
                    .when(authorizationValidator)
                    .validateStoreApplicationRole(managerDetails);

            assertThatThrownBy(() ->
                    storeApplicationService.createStoreApplication(request, managerDetails)
            ).isInstanceOf(CustomException.class);

            verify(authorizationValidator).validateStoreApplicationRole(managerDetails);
            verifyNoInteractions(categoryRepository, regionRepository, userRepository, storeApplicationRepository);
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
}
