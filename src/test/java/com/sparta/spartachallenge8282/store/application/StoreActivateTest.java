package com.sparta.spartachallenge8282.store.application;

import com.sparta.spartachallenge8282.category.domain.CategoryRepository;
import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import com.sparta.spartachallenge8282.menu.domain.MenuRepository;
import com.sparta.spartachallenge8282.region.domain.RegionRepository;
import com.sparta.spartachallenge8282.store.domain.Store;
import com.sparta.spartachallenge8282.store.domain.StoreApplicationRepository;
import com.sparta.spartachallenge8282.store.domain.StoreOperationStatus;
import com.sparta.spartachallenge8282.store.domain.StoreRepository;
import com.sparta.spartachallenge8282.store.presentation.dto.request.StoreOpenStatusRequest;
import com.sparta.spartachallenge8282.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StoreService 영업 관리 테스트")
class StoreActivateTest {

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

    @Mock
    private MenuRepository menuRepository;

    @InjectMocks
    private StoreService storeService;

    private UUID storeId;
    private Long ownerId;
    private UserDetailsImpl ownerDetails;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        ownerId = 1L;

        ownerDetails = new UserDetailsImpl(
                ownerId,
                "owner@test.com",
                "ROLE_OWNER"
        );
    }

    @Nested
    @DisplayName("가게 활성화")
    class ActivateStore {

        @Test
        @DisplayName("PREPARING 상태이고 메뉴가 존재하면 가게 활성화에 성공한다")
        void activateStore_success() {
            // given
            Store store = mock(Store.class);

            when(storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(
                    storeId,
                    ownerId
            )).thenReturn(Optional.of(store));

            when(store.getOperationStatus())
                    .thenReturn(StoreOperationStatus.PREPARING);

            when(menuRepository.existsByStoreIdAndDeletedAtIsNull(storeId))
                    .thenReturn(true);

            // when
            storeService.activateStore(storeId, ownerDetails);

            // then
            verify(store).activate();
            verify(menuRepository)
                    .existsByStoreIdAndDeletedAtIsNull(storeId);
        }

        @Test
        @DisplayName("가게가 존재하지 않으면 활성화할 수 없다")
        void activateStore_storeNotFound() {
            // given
            when(storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(
                    storeId,
                    ownerId
            )).thenReturn(Optional.empty());

            // when & then
            assertThrows(
                    CustomException.class,
                    () -> storeService.activateStore(
                            storeId,
                            ownerDetails
                    )
            );

            verifyNoInteractions(menuRepository);
        }

        @Test
        @DisplayName("PREPARING 상태가 아니면 활성화할 수 없다")
        void activateStore_notPreparing() {
            // given
            Store store = mock(Store.class);

            when(storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(
                    storeId,
                    ownerId
            )).thenReturn(Optional.of(store));

            when(store.getOperationStatus())
                    .thenReturn(StoreOperationStatus.ACTIVE);

            // when & then
            assertThrows(
                    CustomException.class,
                    () -> storeService.activateStore(
                            storeId,
                            ownerDetails
                    )
            );

            verify(store, never()).activate();
            verifyNoInteractions(menuRepository);
        }

        @Test
        @DisplayName("등록된 메뉴가 없으면 가게를 활성화할 수 없다")
        void activateStore_menuNotFound() {
            // given
            Store store = mock(Store.class);

            when(storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(
                    storeId,
                    ownerId
            )).thenReturn(Optional.of(store));

            when(store.getOperationStatus())
                    .thenReturn(StoreOperationStatus.PREPARING);

            when(menuRepository.existsByStoreIdAndDeletedAtIsNull(storeId))
                    .thenReturn(false);

            // when & then
            assertThrows(
                    CustomException.class,
                    () -> storeService.activateStore(
                            storeId,
                            ownerDetails
                    )
            );

            verify(store, never()).activate();
        }

        @Test
        @DisplayName("다른 사용자의 가게는 활성화할 수 없다")
        void activateStore_otherOwner() {
            // given
            when(storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(
                    storeId,
                    ownerId
            )).thenReturn(Optional.empty());

            // when & then
            assertThrows(
                    CustomException.class,
                    () -> storeService.activateStore(
                            storeId,
                            ownerDetails
                    )
            );

            verifyNoInteractions(menuRepository);
        }
    }

    @Nested
    @DisplayName("영업 시작 및 종료")
    class ChangeOpenStatus {

        @Test
        @DisplayName("ACTIVE 상태인 가게는 영업을 시작할 수 있다")
        void openStore_success() {
            // given
            Store store = mock(Store.class);
            StoreOpenStatusRequest request =
                    new StoreOpenStatusRequest(true);

            when(storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(
                    storeId,
                    ownerId
            )).thenReturn(Optional.of(store));

            when(store.getOperationStatus())
                    .thenReturn(StoreOperationStatus.ACTIVE);

            // when
            storeService.changeOpenStatus(
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
            Store store = mock(Store.class);
            StoreOpenStatusRequest request =
                    new StoreOpenStatusRequest(false);

            when(storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(
                    storeId,
                    ownerId
            )).thenReturn(Optional.of(store));

            when(store.getOperationStatus())
                    .thenReturn(StoreOperationStatus.ACTIVE);

            // when
            storeService.changeOpenStatus(
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

            when(storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(
                    storeId,
                    ownerId
            )).thenReturn(Optional.empty());

            // when & then
            assertThrows(
                    CustomException.class,
                    () -> storeService.changeOpenStatus(
                            storeId,
                            request,
                            ownerDetails
                    )
            );
        }

        @Test
        @DisplayName("PREPARING 상태에서는 영업을 시작할 수 없다")
        void changeOpenStatus_preparing() {
            // given
            Store store = mock(Store.class);
            StoreOpenStatusRequest request =
                    new StoreOpenStatusRequest(true);

            when(storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(
                    storeId,
                    ownerId
            )).thenReturn(Optional.of(store));

            when(store.getOperationStatus())
                    .thenReturn(StoreOperationStatus.PREPARING);

            // when & then
            assertThrows(
                    CustomException.class,
                    () -> storeService.changeOpenStatus(
                            storeId,
                            request,
                            ownerDetails
                    )
            );

            verify(store, never()).changeOpenStatus(anyBoolean());
        }

        @Test
        @DisplayName("CLOSE_REQUESTED 상태에서는 영업을 시작할 수 없다")
        void changeOpenStatus_closeRequested() {
            // given
            Store store = mock(Store.class);
            StoreOpenStatusRequest request =
                    new StoreOpenStatusRequest(true);

            when(storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(
                    storeId,
                    ownerId
            )).thenReturn(Optional.of(store));

            when(store.getOperationStatus())
                    .thenReturn(StoreOperationStatus.CLOSE_REQUESTED);

            // when & then
            assertThrows(
                    CustomException.class,
                    () -> storeService.changeOpenStatus(
                            storeId,
                            request,
                            ownerDetails
                    )
            );

            verify(store, never()).changeOpenStatus(anyBoolean());
        }

        @Test
        @DisplayName("다른 사용자의 가게는 영업 상태를 변경할 수 없다")
        void changeOpenStatus_otherOwner() {
            // given
            StoreOpenStatusRequest request =
                    new StoreOpenStatusRequest(true);

            when(storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(
                    storeId,
                    ownerId
            )).thenReturn(Optional.empty());

            // when & then
            assertThrows(
                    CustomException.class,
                    () -> storeService.changeOpenStatus(
                            storeId,
                            request,
                            ownerDetails
                    )
            );
        }
    }
}