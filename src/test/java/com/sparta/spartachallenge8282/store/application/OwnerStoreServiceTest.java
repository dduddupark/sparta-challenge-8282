package com.sparta.spartachallenge8282.store.application;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.security.UserDetailsImpl;
import com.sparta.spartachallenge8282.menu.domain.MenuRepository;
import com.sparta.spartachallenge8282.store.domain.Store;
import com.sparta.spartachallenge8282.store.domain.StoreOperationStatus;
import com.sparta.spartachallenge8282.store.domain.StoreRepository;
import com.sparta.spartachallenge8282.store.presentation.dto.request.StoreOpenStatusRequest;
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
@DisplayName("OwnerStoreService 영업 관리 테스트")
public class OwnerStoreServiceTest {
    @Mock
    private StoreRepository storeRepository;

    @Mock
    private MenuRepository menuRepository;

    @InjectMocks
    private OwnerStoreService ownerStoreService;

    private UUID storeId;
    private Long ownerId;
    private UserDetailsImpl ownerDetails;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        ownerId = 1L;
        ownerDetails = new UserDetailsImpl(ownerId, "owner@test.com", "ROLE_OWNER");
    }

    @Nested
    @DisplayName("가게 활성화")
    class ActivateStore {

        @Test
        @DisplayName("PREPARING 상태이고 메뉴가 존재하면 가게 활성화에 성공한다")
        void activateStore_success() {
            Store store = mock(Store.class);

            when(storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(storeId, ownerId))
                    .thenReturn(Optional.of(store));
            when(store.getOperationStatus()).thenReturn(StoreOperationStatus.PREPARING);
            when(menuRepository.existsByStoreIdAndDeletedAtIsNull(storeId)).thenReturn(true);

            ownerStoreService.activateStore(storeId, ownerDetails);

            verify(store).activate();
            verify(menuRepository).existsByStoreIdAndDeletedAtIsNull(storeId);
        }

        @Test
        @DisplayName("가게가 존재하지 않으면 활성화할 수 없다")
        void activateStore_storeNotFound() {
            when(storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(storeId, ownerId))
                    .thenReturn(Optional.empty());

            assertThrows(CustomException.class,
                    () -> ownerStoreService.activateStore(storeId, ownerDetails));

            verifyNoInteractions(menuRepository);
        }

        @Test
        @DisplayName("PREPARING 상태가 아니면 활성화할 수 없다")
        void activateStore_notPreparing() {
            Store store = mock(Store.class);

            when(storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(storeId, ownerId))
                    .thenReturn(Optional.of(store));
            when(store.getOperationStatus()).thenReturn(StoreOperationStatus.ACTIVE);

            assertThrows(CustomException.class,
                    () -> ownerStoreService.activateStore(storeId, ownerDetails));

            verify(store, never()).activate();
            verifyNoInteractions(menuRepository);
        }

        @Test
        @DisplayName("등록된 메뉴가 없으면 가게를 활성화할 수 없다")
        void activateStore_menuNotFound() {
            Store store = mock(Store.class);

            when(storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(storeId, ownerId))
                    .thenReturn(Optional.of(store));
            when(store.getOperationStatus()).thenReturn(StoreOperationStatus.PREPARING);
            when(menuRepository.existsByStoreIdAndDeletedAtIsNull(storeId)).thenReturn(false);

            assertThrows(CustomException.class,
                    () -> ownerStoreService.activateStore(storeId, ownerDetails));

            verify(store, never()).activate();
        }

        @Test
        @DisplayName("다른 사용자의 가게는 활성화할 수 없다")
        void activateStore_otherOwner() {
            when(storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(storeId, ownerId))
                    .thenReturn(Optional.empty());

            assertThrows(CustomException.class,
                    () -> ownerStoreService.activateStore(storeId, ownerDetails));

            verifyNoInteractions(menuRepository);
        }
    }

    @Nested
    @DisplayName("영업 시작 및 종료")
    class ChangeOpenStatus {

        @Test
        @DisplayName("ACTIVE 상태인 가게는 영업을 시작할 수 있다")
        void openStore_success() {
            Store store = mock(Store.class);
            StoreOpenStatusRequest request = new StoreOpenStatusRequest(true);

            when(storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(storeId, ownerId))
                    .thenReturn(Optional.of(store));
            when(store.getOperationStatus()).thenReturn(StoreOperationStatus.ACTIVE);

            ownerStoreService.changeOpenStatus(storeId, request, ownerDetails);

            verify(store).changeOpenStatus(true);
        }

        @Test
        @DisplayName("ACTIVE 상태인 가게는 영업을 종료할 수 있다")
        void closeStore_success() {
            Store store = mock(Store.class);
            StoreOpenStatusRequest request = new StoreOpenStatusRequest(false);

            when(storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(storeId, ownerId))
                    .thenReturn(Optional.of(store));
            when(store.getOperationStatus()).thenReturn(StoreOperationStatus.ACTIVE);

            ownerStoreService.changeOpenStatus(storeId, request, ownerDetails);

            verify(store).changeOpenStatus(false);
        }

        @Test
        @DisplayName("가게가 존재하지 않으면 영업 상태를 변경할 수 없다")
        void changeOpenStatus_storeNotFound() {
            StoreOpenStatusRequest request = new StoreOpenStatusRequest(true);

            when(storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(storeId, ownerId))
                    .thenReturn(Optional.empty());

            assertThrows(CustomException.class,
                    () -> ownerStoreService.changeOpenStatus(storeId, request, ownerDetails));
        }

        @Test
        @DisplayName("PREPARING 상태에서는 영업을 시작할 수 없다")
        void changeOpenStatus_preparing() {
            Store store = mock(Store.class);
            StoreOpenStatusRequest request = new StoreOpenStatusRequest(true);

            when(storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(storeId, ownerId))
                    .thenReturn(Optional.of(store));
            when(store.getOperationStatus()).thenReturn(StoreOperationStatus.PREPARING);

            assertThrows(CustomException.class,
                    () -> ownerStoreService.changeOpenStatus(storeId, request, ownerDetails));

            verify(store, never()).changeOpenStatus(anyBoolean());
        }

        @Test
        @DisplayName("CLOSE_REQUESTED 상태에서는 영업을 시작할 수 없다")
        void changeOpenStatus_closeRequested() {
            Store store = mock(Store.class);
            StoreOpenStatusRequest request = new StoreOpenStatusRequest(true);

            when(storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(storeId, ownerId))
                    .thenReturn(Optional.of(store));
            when(store.getOperationStatus()).thenReturn(StoreOperationStatus.CLOSE_REQUESTED);

            assertThrows(CustomException.class,
                    () -> ownerStoreService.changeOpenStatus(storeId, request, ownerDetails));

            verify(store, never()).changeOpenStatus(anyBoolean());
        }

        @Test
        @DisplayName("다른 사용자의 가게는 영업 상태를 변경할 수 없다")
        void changeOpenStatus_otherOwner() {
            StoreOpenStatusRequest request = new StoreOpenStatusRequest(true);

            when(storeRepository.findByIdAndOwner_IdAndDeletedAtIsNull(storeId, ownerId))
                    .thenReturn(Optional.empty());

            assertThrows(CustomException.class,
                    () -> ownerStoreService.changeOpenStatus(storeId, request, ownerDetails));
        }
    }
}
