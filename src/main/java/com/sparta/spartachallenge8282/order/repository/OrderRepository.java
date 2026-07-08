package com.sparta.spartachallenge8282.order.repository;

import com.sparta.spartachallenge8282.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/*
 * 주문 Repository
 * 책임:
 * - Order 엔티티 저장, 조회
 */
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /*
     * 삭제되지 않은 주문만 조회한다.
     * BaseEntity의 deletedAt이 null이면 삭제되지 않은 데이터로 본다.
     */
    Optional<Order> findByIdAndDeletedAtIsNull(UUID orderId);
}