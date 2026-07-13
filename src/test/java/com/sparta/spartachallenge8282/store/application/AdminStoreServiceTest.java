package com.sparta.spartachallenge8282.store.application;

import com.sparta.spartachallenge8282.category.domain.Category;
import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import com.sparta.spartachallenge8282.region.domain.Region;
import com.sparta.spartachallenge8282.store.application.validator.StoreAuthorizationValidator;
import com.sparta.spartachallenge8282.store.domain.*;
import com.sparta.spartachallenge8282.store.presentation.dto.request.StoreRejectRequest;
import com.sparta.spartachallenge8282.store.presentation.dto.response.StoreApplicationProcessResponse;
import com.sparta.spartachallenge8282.user.entity.User;
import com.sparta.spartachallenge8282.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminStoreService 테스트")
class AdminStoreServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreApplicationRepository storeApplicationRepository;

    @Mock
    private StoreAuthorizationValidator authorizationValidator;

    @InjectMocks
    private AdminStoreService adminStoreService;

    private UserDetailsImpl managerDetails;
    private User applicant;
    private Category category;
    private Region region;
    private UUID applicationId;
    private UUID storeId;

    @BeforeEach
    void setUp() {
        managerDetails = new UserDetailsImpl(2L, "manager@test.com", UserRole.MANAGER.getAuthority());
        applicant = mock(User.class);
        category = mock(Category.class);
        region = mock(Region.class);
        applicationId = UUID.randomUUID();
        storeId = UUID.randomUUID();

        lenient().when(applicant.getId()).thenReturn(1L);
        lenient().when(applicant.getEmail()).thenReturn("customer@test.com");
        lenient().when(applicant.getNickname()).thenReturn("테스트사용자");
        lenient().when(applicant.getRole()).thenReturn(UserRole.CUSTOMER);
    }

    @Nested
    @DisplayName("가게 등록 승인")
    class ApproveStoreTest {

        @Test
        @DisplayName("PENDING 신청을 승인하면 Store가 생성되고 신청자는 OWNER로 승격된다")
        void approveStore_success() {
            StoreApplication application = mockApplication(StoreApplicationStatus.PENDING);
            Store savedStore = mockSavedStore();

            when(storeApplicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
            when(storeRepository.save(any(Store.class))).thenReturn(savedStore);

            StoreApplicationProcessResponse response =
                    adminStoreService.approveStore(applicationId, managerDetails);

            assertThat(response).isNotNull();
            verify(authorizationValidator).validateManagerRole(managerDetails);
            verify(application).approve();
            verify(storeRepository).save(any(Store.class));
            verify(applicant).promoteToOwner();
        }

        @Test
        @DisplayName("이미 처리된 신청은 승인할 수 없다")
        void approveStore_invalidStatus() {
            StoreApplication application = mockApplication(StoreApplicationStatus.APPROVED);
            when(storeApplicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

            assertThatThrownBy(() -> adminStoreService.approveStore(applicationId, managerDetails))
                    .isInstanceOf(CustomException.class);

            verify(application, never()).approve();
            verify(storeRepository, never()).save(any(Store.class));
            verify(applicant, never()).promoteToOwner();
        }

        @Test
        @DisplayName("존재하지 않는 신청은 승인할 수 없다")
        void approveStore_applicationNotFound() {
            when(storeApplicationRepository.findById(applicationId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminStoreService.approveStore(applicationId, managerDetails))
                    .isInstanceOf(CustomException.class);

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
            StoreApplication application = mockApplication(StoreApplicationStatus.PENDING);
            StoreRejectRequest request = new StoreRejectRequest("사업자 정보가 일치하지 않습니다.");

            when(storeApplicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

            StoreApplicationProcessResponse response =
                    adminStoreService.rejectStore(applicationId, request, managerDetails);

            assertThat(response).isNotNull();
            verify(authorizationValidator).validateManagerRole(managerDetails);
            verify(application).reject("사업자 정보가 일치하지 않습니다.");
            verify(storeRepository, never()).save(any(Store.class));
            verify(applicant, never()).promoteToOwner();
        }

        @Test
        @DisplayName("거절 사유가 공백이면 거절할 수 없다")
        void rejectStore_blankReason() {
            StoreApplication application = mockApplication(StoreApplicationStatus.PENDING);
            StoreRejectRequest request = new StoreRejectRequest(" ");

            when(storeApplicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

            assertThatThrownBy(() -> adminStoreService.rejectStore(applicationId, request, managerDetails))
                    .isInstanceOf(CustomException.class);

            verify(application, never()).reject(anyString());
            verify(storeRepository, never()).save(any(Store.class));
        }

        @Test
        @DisplayName("이미 처리된 신청은 거절할 수 없다")
        void rejectStore_invalidStatus() {
            StoreApplication application = mockApplication(StoreApplicationStatus.APPROVED);
            StoreRejectRequest request = new StoreRejectRequest("거절 사유");

            when(storeApplicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

            assertThatThrownBy(() -> adminStoreService.rejectStore(applicationId, request, managerDetails))
                    .isInstanceOf(CustomException.class);

            verify(application, never()).reject(anyString());
        }
    }

    private StoreApplication mockApplication(StoreApplicationStatus status) {
        StoreApplication application = mock(StoreApplication.class);

        lenient().when(application.getId()).thenReturn(applicationId);
        lenient().when(application.getStatus()).thenReturn(status);
        lenient().when(application.getApplicant()).thenReturn(applicant);
        lenient().when(application.getCategory()).thenReturn(category);
        lenient().when(application.getRegion()).thenReturn(region);
        lenient().when(application.getStoreName()).thenReturn("테스트가게");
        lenient().when(application.getStoreTel()).thenReturn("010-1234-5678");
        lenient().when(application.getStoreImage()).thenReturn(null);
        lenient().when(application.getAddress()).thenReturn("서울특별시 강남구 테헤란로 123");
        lenient().when(application.getMinOrderPrice()).thenReturn(15000);
        lenient().when(application.getDeliveryFee()).thenReturn(3000);
        lenient().when(application.getFreeDeliveryAmount()).thenReturn(30000);
        lenient().when(application.getOpenTime()).thenReturn(LocalTime.of(9, 0));
        lenient().when(application.getCloseTime()).thenReturn(LocalTime.of(22, 0));
        return application;
    }

    private Store mockSavedStore() {
        Store store = mock(Store.class);
        lenient().when(store.getId()).thenReturn(storeId);
        lenient().when(store.getStoreName()).thenReturn("테스트가게");
        lenient().when(store.getOwner()).thenReturn(applicant);
        lenient().when(store.getCategory()).thenReturn(category);
        lenient().when(store.getRegion()).thenReturn(region);
        lenient().when(store.getStoreTel()).thenReturn("010-1234-5678");
        lenient().when(store.getAddress()).thenReturn("서울특별시 강남구 테헤란로 123");
        return store;
    }
}