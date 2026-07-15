package com.sparta.spartachallenge8282.order.service;

import com.sparta.spartachallenge8282.global.exception.CustomException;
import com.sparta.spartachallenge8282.global.exception.ErrorCode;
import com.sparta.spartachallenge8282.menu.domain.Menu;
import com.sparta.spartachallenge8282.menu.domain.MenuBadge;
import com.sparta.spartachallenge8282.menu.domain.MenuRepository;
import com.sparta.spartachallenge8282.menu.domain.MenuStatus;
import com.sparta.spartachallenge8282.option.domain.MenuOption;
import com.sparta.spartachallenge8282.option.domain.MenuOptionRepository;
import com.sparta.spartachallenge8282.optiongroup.domain.MenuOptionGroup;
import com.sparta.spartachallenge8282.optiongroup.domain.MenuOptionGroupRepository;
import com.sparta.spartachallenge8282.order.application.OrderService;
import com.sparta.spartachallenge8282.order.presentation.dto.request.OrderCreateRequestDto;
import com.sparta.spartachallenge8282.order.presentation.dto.request.OrderItemRequestDto;
import com.sparta.spartachallenge8282.order.domain.Order;
import com.sparta.spartachallenge8282.order.domain.OrderItem;
import com.sparta.spartachallenge8282.order.domain.OrderRepository;
import com.sparta.spartachallenge8282.order.domain.OrderStatusHistoryRepository;
import com.sparta.spartachallenge8282.payment.application.PaymentService;
import com.sparta.spartachallenge8282.store.domain.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OrderService의 주문 생성 로직을 검증하는 단위 테스트
 * 실제 DB를 사용하지 않고 Repository를 Mock 객체로 대체한다.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    /**
     * 주문 저장을 담당하는 가짜 Repository
     */
    @Mock
    private OrderRepository orderRepository;

    /**
     * 주문 상태 이력 저장을 담당하는 가짜 Repository
     */
    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    private StoreRepository storeRepository;
    /**
     *  paymentService
     */
    @Mock
    private PaymentService paymentService;

    /**
     * 메뉴 조회를 담당하는 가짜 Repository
     */
    @Mock
    private MenuRepository menuRepository;



    /**
     * 선택한 메뉴 옵션 조회를 담당하는 가짜 Repository
     */
    @Mock
    private MenuOptionRepository menuOptionRepository;

    /**
     * 메뉴별 옵션 그룹 조회를 담당하는 가짜 Repository
     */
    @Mock
    private MenuOptionGroupRepository menuOptionGroupRepository;



    /**
     * 실제로 테스트할 대상 객체
     */
    private OrderService orderService;

    /**
     * 여러 테스트에서 공통으로 사용할 테스트 데이터
     */
    private Long customerId;
    private UUID storeId;
    private UUID menuId;

    /**
     * 각 테스트가 실행되기 전에 호출된다.
     * Mock Repository를 주입해 OrderService를 직접 생성하고
     * 테스트에 사용할 기본 ID 값을 준비한다.
     */
    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository,
                orderStatusHistoryRepository,
                storeRepository,
                paymentService,
                menuRepository,
                menuOptionRepository,
                menuOptionGroupRepository
                menuRepository
        );

        customerId = 1L;
        storeId = UUID.randomUUID();
        menuId = UUID.randomUUID();
    }

    /**
     * 정상 주문 생성 테스트
     * 판매 중이고 숨김 처리되지 않은 메뉴를 주문하면
     * 주문과 주문 상품이 올바르게 생성되는지 검증한다.
     */
    @Test
    @DisplayName("판매 중인 메뉴를 이용해 주문을 생성할 수 있다")
    void createOrder_success() {

        // DB에서 조회됐다고 가정할 판매 중인 메뉴 생성
        Menu menu = createMenu(
                menuId,
                storeId,
                "테스트 불고기버거",
                8000,
                MenuStatus.ON_SALE,
                false
        );

        // 불고기버거 2개를 주문하는 요청 데이터 생성
        OrderCreateRequestDto request = createOrderRequest(
                storeId,
                menuId,
                2
        );

        // 메뉴 조회 메서드가 호출되면 위에서 만든 메뉴를 반환하도록 설정
        when(menuRepository.findByIdAndDeletedAtIsNull(menuId))
                .thenReturn(Optional.of(menu));

        // save()가 호출되면 전달받은 Order 객체를 그대로 반환
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 실제 주문 생성 서비스 호출
        orderService.createOrder(customerId, request);

        // save()에 전달된 Order 객체를 꺼내기 위한 캡처 객체
        ArgumentCaptor<Order> orderCaptor =
                ArgumentCaptor.forClass(Order.class);

        // orderRepository.save()가 호출됐는지 확인하고 전달된 Order를 캡처
        verify(orderRepository).save(orderCaptor.capture());

        // save()에 전달된 주문 객체 가져오기
        Order savedOrder = orderCaptor.getValue();

        // 주문한 고객 ID가 올바른지 확인
        assertThat(savedOrder.getUserId())
                .isEqualTo(customerId);

        // 주문한 가게 ID가 올바른지 확인
        assertThat(savedOrder.getStoreId())
                .isEqualTo(storeId);

        // 메뉴 금액: 8000원 × 2개 = 16000원
        assertThat(savedOrder.getMenuTotalPrice())
                .isEqualTo(16000);

        // 배송비가 3000원인지 확인
        assertThat(savedOrder.getDeliveryFee())
                .isEqualTo(3000);

        // 할인 금액이 0원인지 확인
        assertThat(savedOrder.getDiscountAmount())
                .isZero();

        // 총 주문 금액: 메뉴 16000원 + 배송비 3000원 = 19000원
        assertThat(savedOrder.getTotalPrice())
                .isEqualTo(19000);

        // 주문 상품이 1종류 생성됐는지 확인
        assertThat(savedOrder.getOrderItems())
                .hasSize(1);

        // 생성된 주문 상품 가져오기
        OrderItem savedOrderItem =
                savedOrder.getOrderItems().get(0);

        // 주문 상품에 저장된 메뉴 ID 확인
        assertThat(savedOrderItem.getMenuId())
                .isEqualTo(menuId);

        // 주문 시점의 메뉴 이름이 저장됐는지 확인
        assertThat(savedOrderItem.getMenuName())
                .isEqualTo("테스트 불고기버거");

        // 주문 시점의 메뉴 가격이 저장됐는지 확인
        assertThat(savedOrderItem.getMenuPrice())
                .isEqualTo(8000);

        // 주문 수량 확인
        assertThat(savedOrderItem.getQuantity())
                .isEqualTo(2);

        // 주문 상품 금액: 8000원 × 2개 = 16000원
        assertThat(savedOrderItem.getTotalPrice())
                .isEqualTo(16000);

        // OrderItem이 생성된 Order를 참조하는지 확인
        assertThat(savedOrderItem.getOrder())
                .isSameAs(savedOrder);
    }

    /**
     * 존재하지 않는 메뉴 주문 실패 테스트
     */
    @Test
    @DisplayName("존재하지 않는 메뉴로 주문하면 실패한다")
    void createOrder_fail_menuNotFound() {

        // 존재하지 않는 메뉴를 주문하는 요청 생성
        OrderCreateRequestDto request = createOrderRequest(
                storeId,
                menuId,
                1
        );

        // 메뉴 조회 결과가 없도록 설정
        when(menuRepository.findByIdAndDeletedAtIsNull(menuId))
                .thenReturn(Optional.empty());

        // 주문 생성 시 MENU_NOT_FOUND 예외가 발생하는지 확인
        assertThatThrownBy(() ->
                orderService.createOrder(customerId, request)
        )
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> {
                    CustomException customException =
                            (CustomException) exception;

                    assertThat(customException.getErrorCode())
                            .isEqualTo(ErrorCode.MENU_NOT_FOUND);
                });

        // 예외가 발생했으므로 주문 저장은 호출되면 안 된다
        verify(orderRepository, never())
                .save(any(Order.class));
    }

    /**
     * 요청한 가게와 메뉴의 가게가 다른 경우 실패 테스트
     */
    @Test
    @DisplayName("다른 가게의 메뉴로 주문하면 실패한다")
    void createOrder_fail_menuStoreMismatch() {

        // 주문 요청의 가게와 다른 가게 ID
        UUID otherStoreId = UUID.randomUUID();

        // 다른 가게에 속한 메뉴 생성
        Menu menu = createMenu(
                menuId,
                otherStoreId,
                "다른 가게 메뉴",
                9000,
                MenuStatus.ON_SALE,
                false
        );

        // 원래 storeId로 주문 요청 생성
        OrderCreateRequestDto request = createOrderRequest(
                storeId,
                menuId,
                1
        );

        // 메뉴 조회 시 다른 가게의 메뉴를 반환
        when(menuRepository.findByIdAndDeletedAtIsNull(menuId))
                .thenReturn(Optional.of(menu));

        // 메뉴의 가게와 요청 가게가 다르므로 예외가 발생해야 한다
        assertThatThrownBy(() ->
                orderService.createOrder(customerId, request)
        )
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> {
                    CustomException customException =
                            (CustomException) exception;

                    assertThat(customException.getErrorCode())
                            .isEqualTo(ErrorCode.MENU_STORE_MISMATCH);
                });

        // 유효하지 않은 주문이므로 저장하면 안 된다
        verify(orderRepository, never())
                .save(any(Order.class));
    }

    /**
     * 숨김 처리된 메뉴 주문 실패 테스트
     */
    @Test
    @DisplayName("숨김 처리된 메뉴로 주문하면 실패한다")
    void createOrder_fail_hiddenMenu() {

        // 숨김 처리된 메뉴 생성
        Menu menu = createMenu(
                menuId,
                storeId,
                "숨김 테스트 메뉴",
                7000,
                MenuStatus.ON_SALE,
                true
        );

        // 숨김 메뉴 주문 요청 생성
        OrderCreateRequestDto request = createOrderRequest(
                storeId,
                menuId,
                1
        );

        // 메뉴 조회 시 숨김 처리된 메뉴 반환
        when(menuRepository.findByIdAndDeletedAtIsNull(menuId))
                .thenReturn(Optional.of(menu));

        // 숨김 메뉴는 주문할 수 없으므로 예외가 발생해야 한다
        assertThatThrownBy(() ->
                orderService.createOrder(customerId, request)
        )
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> {
                    CustomException customException =
                            (CustomException) exception;

                    assertThat(customException.getErrorCode())
                            .isEqualTo(
                                    ErrorCode.HIDDEN_MENU_NOT_ORDERABLE
                            );
                });

        // 주문 저장은 실행되면 안 된다
        verify(orderRepository, never())
                .save(any(Order.class));
    }

    /**
     * 품절된 메뉴 주문 실패 테스트
     */
    @Test
    @DisplayName("품절된 메뉴로 주문하면 실패한다")
    void createOrder_fail_soldOutMenu() {

        // 품절 상태의 메뉴 생성
        Menu menu = createMenu(
                menuId,
                storeId,
                "품절 테스트 메뉴",
                10000,
                MenuStatus.SOLD_OUT,
                false
        );

        // 품절 메뉴 주문 요청 생성
        OrderCreateRequestDto request = createOrderRequest(
                storeId,
                menuId,
                1
        );

        // 메뉴 조회 시 품절 메뉴 반환
        when(menuRepository.findByIdAndDeletedAtIsNull(menuId))
                .thenReturn(Optional.of(menu));

        // 품절 메뉴는 주문할 수 없으므로 예외가 발생해야 한다
        assertThatThrownBy(() ->
                orderService.createOrder(customerId, request)
        )
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> {
                    CustomException customException =
                            (CustomException) exception;

                    assertThat(customException.getErrorCode())
                            .isEqualTo(ErrorCode.MENU_NOT_ORDERABLE);
                });

        // 주문 저장은 호출되면 안 된다
        verify(orderRepository, never())
                .save(any(Order.class));
    }

    /**
     * 테스트에서 반복적으로 사용할 주문 요청 객체 생성 메서드
     */
    private OrderCreateRequestDto createOrderRequest(
            UUID requestStoreId,
            UUID requestMenuId,
            int quantity
    ) {

        // 주문할 메뉴 ID, 수량, 메뉴 옵션을 담는다
        OrderItemRequestDto orderItem =
                new OrderItemRequestDto(
                        requestMenuId,
                        quantity,
                        List.of()
                );

        // 가게 정보, 배송지, 요청사항, 주문 상품 목록을 담아 요청 객체 생성
        return new OrderCreateRequestDto(
                requestStoreId,
                "서울특별시 종로구 세종대로 175",
                "101동 1001호",
                "문 앞에 놓아주세요.",
                List.of(orderItem)
        );
    }

    /**
     * 테스트에서 사용할 Menu 객체 생성 메서드
     */
    private Menu createMenu(
            UUID id,
            UUID menuStoreId,
            String name,
            int price,
            MenuStatus status,
            boolean hidden
    ) {

        // 테스트 조건에 맞는 Menu 객체 생성
        Menu menu = Menu.builder()
                .storeId(menuStoreId)
                .name(name)
                .description("주문 테스트 메뉴")
                .price(price)
                .sortOrder(1)
                .status(status)
                .badge(MenuBadge.NONE)
                .isHidden(hidden)
                .build();

        /*
         * Menu의 ID가 private이고 직접 설정하는 메서드가 없기 때문에
         * 테스트에서 Reflection을 이용해 ID를 강제로 설정한다.
         */
        ReflectionTestUtils.setField(
                menu,
                "id",
                id
        );

        return menu;
    }

    /**
     * 여러 메뉴를 함께 주문했을 때 금액 합산 검증
     */
    @Test
    @DisplayName("여러 메뉴의 상품 금액을 합산하여 주문을 생성한다")
    void createOrder_multipleMenus_success() {

        // 서로 다른 두 메뉴의 ID 생성
        UUID firstMenuId = UUID.randomUUID();
        UUID secondMenuId = UUID.randomUUID();

        // 첫 번째 메뉴: 8000원
        Menu firstMenu = createMenu(
                firstMenuId,
                storeId,
                "불고기버거",
                8000,
                MenuStatus.ON_SALE,
                false
        );

        // 두 번째 메뉴: 9000원
        Menu secondMenu = createMenu(
                secondMenuId,
                storeId,
                "치즈버거",
                9000,
                MenuStatus.ON_SALE,
                false
        );

        // 불고기버거 2개와 치즈버거 1개를 주문하는 요청 생성
        OrderCreateRequestDto request =
                new OrderCreateRequestDto(
                        storeId,
                        "서울특별시 종로구",
                        "101동 1001호",
                        "문 앞에 놓아주세요.",
                        List.of(
                                new OrderItemRequestDto(
                                        firstMenuId,
                                        2,
                                        List.of()
                                ),
                                new OrderItemRequestDto(
                                        secondMenuId,
                                        1,
                                        List.of()
                                )
                        )
                );

        // 첫 번째 메뉴 조회 결과 설정
        when(menuRepository.findByIdAndDeletedAtIsNull(firstMenuId))
                .thenReturn(Optional.of(firstMenu));

        // 두 번째 메뉴 조회 결과 설정
        when(menuRepository.findByIdAndDeletedAtIsNull(secondMenuId))
                .thenReturn(Optional.of(secondMenu));

        // 저장된 Order 객체를 그대로 반환하도록 설정
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        // 주문 생성 서비스 실행
        orderService.createOrder(customerId, request);

        // Repository에 전달된 Order 객체를 확인하기 위한 Captor
        ArgumentCaptor<Order> captor =
                ArgumentCaptor.forClass(Order.class);

        // save()에 전달된 주문 객체 캡처
        verify(orderRepository).save(captor.capture());

        Order savedOrder = captor.getValue();

        // 주문 상품이 2종류 생성됐는지 확인
        assertThat(savedOrder.getOrderItems())
                .hasSize(2);

        /*
         * 불고기버거: 8000원 × 2개 = 16000원
         * 치즈버거: 9000원 × 1개 = 9000원
         * 메뉴 합계: 25000원
         */
        assertThat(savedOrder.getMenuTotalPrice())
                .isEqualTo(25000);

        // 메뉴 합계 25000원 + 배송비 3000원 = 총 28000원
        assertThat(savedOrder.getTotalPrice())
                .isEqualTo(28000);
    }

    @Test
    @DisplayName("메뉴 옵션을 포함하여 주문을 생성할 수 있다")
    void createOrder_withOption_success() {
        // given

        // 1. 주문할 메뉴 생성
        Menu menu = createMenu(
                menuId,
                storeId,
                "불고기버거",
                8000,
                MenuStatus.ON_SALE,
                false
        );

        // 2. 옵션 그룹과 옵션 ID 준비
        UUID optionGroupId = UUID.randomUUID();
        UUID cheeseOptionId = UUID.randomUUID();

        /*
         * 옵션 그룹 생성
         *
         * 추가 선택 그룹
         * - 필수 아님
         * - 최소 0개
         * - 최대 2개
         * - 활성 상태
         */
        MenuOptionGroup optionGroup =
                MenuOptionGroup.builder()
                        .menuId(menuId)
                        .name("추가 선택")
                        .isRequired(false)
                        .minSelect(0)
                        .maxSelect(2)
                        .sortOrder(1)
                        .isActive(true)
                        .build();

        ReflectionTestUtils.setField(
                optionGroup,
                "id",
                optionGroupId
        );

        /*
         * 치즈 추가 옵션 생성
         * 추가 금액: 1,000원
         */
        MenuOption cheeseOption =
                MenuOption.builder()
                        .optionGroupId(optionGroupId)
                        .name("치즈 추가")
                        .additionalPrice(1000)
                        .sortOrder(1)
                        .isActive(true)
                        .build();

        ReflectionTestUtils.setField(
                cheeseOption,
                "id",
                cheeseOptionId
        );

        // 3. 메뉴 2개와 치즈 옵션을 주문하는 요청 생성
        OrderItemRequestDto orderItemRequest =
                new OrderItemRequestDto(
                        menuId,
                        2,
                        List.of(cheeseOptionId)
                );

        OrderCreateRequestDto request =
                new OrderCreateRequestDto(
                        storeId,
                        "서울특별시 종로구",
                        "101동 1001호",
                        "문 앞에 놓아주세요.",
                        List.of(orderItemRequest)
                );

        // 4. Repository Mock 동작 설정
        when(menuRepository.findByIdAndDeletedAtIsNull(menuId))
                .thenReturn(Optional.of(menu));

        when(menuOptionGroupRepository
                .findAllByMenuIdAndDeletedAtIsNull(menuId))
                .thenReturn(List.of(optionGroup));

        when(menuOptionRepository
                .findAllByIdInAndDeletedAtIsNull(
                        List.of(cheeseOptionId)
                ))
                .thenReturn(List.of(cheeseOption));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        // when
        orderService.createOrder(customerId, request);

        // then

        // 실제 save()에 전달된 Order를 가져온다.
        ArgumentCaptor<Order> orderCaptor =
                ArgumentCaptor.forClass(Order.class);

        verify(orderRepository)
                .save(orderCaptor.capture());

        Order savedOrder = orderCaptor.getValue();

        // 주문 상품은 한 종류
        assertThat(savedOrder.getOrderItems())
                .hasSize(1);

        OrderItem savedOrderItem =
                savedOrder.getOrderItems().get(0);

        /*
         * 계산:
         * (메뉴 8,000원 + 치즈 옵션 1,000원) × 수량 2개
         * = 18,000원
         */
        assertThat(savedOrderItem.getTotalPrice())
                .isEqualTo(18000);

        // 주문 메뉴 총액에도 옵션 금액이 포함되어야 한다.
        assertThat(savedOrder.getMenuTotalPrice())
                .isEqualTo(18000);

        /*
         * 총 주문 금액:
         * 메뉴 및 옵션 금액 18,000원
         * + 배달비 3,000원
         * = 21,000원
         */
        assertThat(savedOrder.getTotalPrice())
                .isEqualTo(21000);

        // 선택한 옵션이 주문 상품에 저장됐는지 확인
        assertThat(savedOrderItem.getOptions())
                .hasSize(1);

        assertThat(savedOrderItem.getOptions().get(0).getMenuOptionId())
                .isEqualTo(cheeseOptionId);

        assertThat(savedOrderItem.getOptions().get(0).getOptionGroupId())
                .isEqualTo(optionGroupId);

        assertThat(savedOrderItem.getOptions().get(0).getOptionGroupName())
                .isEqualTo("추가 선택");

        assertThat(savedOrderItem.getOptions().get(0).getOptionName())
                .isEqualTo("치즈 추가");

        assertThat(savedOrderItem.getOptions().get(0).getAdditionalPrice())
                .isEqualTo(1000);

        // 양방향 연관관계 확인
        assertThat(savedOrderItem.getOptions().get(0).getOrderItem())
                .isSameAs(savedOrderItem);
    }
}